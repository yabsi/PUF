package dataTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one response created by a user
 */
public class Response implements Serializable {
    private static final long serialVersionUID = -292775056595225846L;

    // List of points which the user swiped
    // always maintains the origional response pattern
    private ArrayList<Point> responsePattern;

    // normalized list of points. This will be overridden every time normalize
    // is called
    private ArrayList<Point> normalizedResponsePattern;

    // Count of motion events this response originally had
    private int motionEvenCount;

    public Response(List<Point> responsePattern) {
        this.responsePattern = new ArrayList<Point>(responsePattern);
        motionEvenCount = responsePattern.size();

        this.normalizedResponsePattern = this.responsePattern;
        // ArrayList<Point>(responsePattern);
    }

    /**
     * return the normalized response pattern
     */
    public List<Point> getOrigionalResponse() {
        return this.responsePattern;
        // return this.normalizedResponsePattern;
    }

    /**
     * return the normalized response pattern
     */
    public List<Point> getNormalizedResponse() {
        return this.normalizedResponsePattern;
    }

    public int getMotionEvenCount() {
        return motionEvenCount;
    }

    /**
     * returns the time of the last point minus the time of the first point.
     * This method preforms the function literally. the time mig variable will
     * be changed during normalization. After the point is normalized this will
     * most likely return a different value compared to before the response
     * being normalized.
     */
    public double getTimeLength() {
        return responsePattern.get(responsePattern.size() - 1).getTime() - responsePattern.get(0).getTime();
    }

    /**
     * remove all duplicate points from the response pattern.
     * Points will only be compared in terms of x and y.
     * <p>
     * return the number of duplicates removed
     */
    public int remove_duplicates() {
        int removed_count = 0;

        // filter response_points
        if (this.responsePattern.size() > 1) {
            Point prev_point;
            Point current_point;
            int i = 1;

            // go though list of points checking that neighbors are equal
            while (i < responsePattern.size()) {
                prev_point = responsePattern.get(i - 1);
                current_point = responsePattern.get(i);

                // check equality
                if (locations_equal(current_point, prev_point)) {
                    // points are equal, throw current point out
                    responsePattern.remove(i);
                    removed_count++;
                } else {
                    i++;
                }
            }
        }

        return removed_count;
    }

    /**
     * determines if x1 == x2 and y1 == y2
     */
    private boolean locations_equal(Point p1, Point p2) {
        double episilon = .001;
        return (Math.abs(p2.getX() - p1.getX()) < episilon) && (Math.abs(p2.getY() - p1.getY()) < episilon);
    }

    /**
     * Normalizes current Response Pattern to points within normalizingPoints; Interpolates values for
     * pressure, distance, time, etc.
     *
     * @param normalizingPoints List of points for the response to normalize to
     */
    public void normalize(List<Point> normalizingPoints) {
        ArrayList<Point> newNormalizedList = new ArrayList<>();

        double xTransform, yTransform; // For moving response points to align with normalizingPoints
        double theta, newX, newY, newPressure, newDistance, newTime, newVelocity, newAcceleration; // Values to use in creating normalizedResponsePattern
        double traceDistance; // Euclidean distance from entire current responsePattern
        double deltaD; // Distance between each of the normalizing points
        double remainingDistance; // Used to keep a running total of how far along the next point has gone
        double d; // Used when needing to interpolate more points if the trace isn't as long as list of normalizingPoints
        double cumulativeTime = 0; // Total time between normalized points
        double prevCumulativeTime = 0; // Time used to calculated normalized time; is subtracted from total time
        double interpolated_time; // Total Time / number of points, to be used to add points' time values when interpolating

        int numExtraNormalizingPoints; // Number of new Normalizing Points the current trace covers
        int normalizingPointsLength = normalizingPoints.size();
        int responseLength = responsePattern.size();
        int i, j; // i counts indices of normalizingPoints, j counts indices of responsePattern

        Point prevPoint, curPoint; // Previous and current trace points when looping through response

        // preform the transformation, add first point to new Normalized List
        xTransform = normalizingPoints.get(0).getX() - responsePattern.get(0).getX();
        yTransform = normalizingPoints.get(0).getY() - responsePattern.get(0).getY();
        transform_response(responsePattern, xTransform, yTransform);
        newNormalizedList.add(responsePattern.get(0));

        // Catch if normalizingTrace is only 1 point (hopefully never happens)
        if (normalizingPoints.size() == 1) {
            this.normalizedResponsePattern = newNormalizedList;
            return;
        }

        // Distance to walk each point to normalized point
        deltaD = computeEuclideanDistance(normalizingPoints.get(0), normalizingPoints.get(1));
        traceDistance = computeTraceDistance();
        numExtraNormalizingPoints = (int) Math.floor((traceDistance / deltaD) + 1);

        // added to handle the case where the response deltaD is longer than normalizingPoints
        numExtraNormalizingPoints = (numExtraNormalizingPoints <= normalizingPoints.size()) ? numExtraNormalizingPoints : normalizingPoints.size();

        remainingDistance = deltaD;
        j = 1;

        // this loop will run NL-1 times
        for (i = 1; i < numExtraNormalizingPoints; i++) {
            prevPoint = responsePattern.get(j - 1);
            curPoint = responsePattern.get(j);

            while (computeEuclideanDistance(prevPoint, curPoint) < remainingDistance) {
                if (j >= responsePattern.size()) {
                    this.normalizedResponsePattern = newNormalizedList;
                    return;
                }
                remainingDistance -= computeEuclideanDistance(prevPoint, curPoint);
                cumulativeTime += curPoint.getTime();
                j++;
                prevPoint = responsePattern.get(j - 1);
                curPoint = responsePattern.get(j);
            }

            // Subtract off the last time added
            cumulativeTime -= curPoint.getTime();

            double x_difference = (curPoint.getX() - prevPoint.getX());
            double x_sine = 1;
            // check that the difference isn't equal to zero to ensure no divide by 0 error
            if (!(x_difference == 0)) {
                // this will make x_sine +1 or -1 depending on the sine
                x_sine = x_difference / Math.abs(x_difference);
            }

            // Now the point we are looking for is between responsePattern[j - 1] and responsePattern[j]
            theta = Math.atan((curPoint.getY() - prevPoint.getY()) / (curPoint.getX() - prevPoint.getX()));
            newX = prevPoint.getX() + (remainingDistance * Math.cos(theta) * x_sine);
            newY = prevPoint.getY() + (remainingDistance * Math.sin(theta) * x_sine);

            //Interpolate pressure and other attributes
            /* pressure */
            newPressure = prevPoint.getPressure() + ((remainingDistance / computeEuclideanDistance(prevPoint, curPoint)) * (curPoint.getPressure() - prevPoint.getPressure()));
            /* distance */
            newDistance = computeEuclideanDistance(new Point(newX, newY, 0), curPoint);
            /* time */
            newTime = cumulativeTime + (curPoint.getTime() * (remainingDistance / computeEuclideanDistance(prevPoint, curPoint))) - prevCumulativeTime;
            /* velocity */
            newVelocity = 1.0;
            /* acceleration */
            newAcceleration = 1.0;

            Point p = new Point(newX, newY);
            p.set_metric(Point.Metrics.PRESSURE, newPressure);
            p.set_metric(Point.Metrics.DISTANCE, newDistance);
            p.set_metric(Point.Metrics.TIME, newTime);
            p.set_metric(Point.Metrics.VELOCITY, newVelocity);
            p.set_metric(Point.Metrics.ACCELERATION, newAcceleration);

            newNormalizedList.add(p);

            remainingDistance = deltaD + computeEuclideanDistance(prevPoint, newNormalizedList.get(i));

            prevCumulativeTime = cumulativeTime;
        }

        // this breaks things because if we have any points left to do and we don't do them then our normalized response lists will be differant sizes.
        // This means there will be an arrayIndexOoutOfBounds error when we get to the part of the code where we find mu. if >= 1
        if (responsePattern.size() <= 1) {
            this.normalizedResponsePattern = newNormalizedList;
            return;
        }

        // Now take care of remaining (NL - N) points which we need to interpolate
        prevPoint = responsePattern.get(responseLength - 2);
        curPoint = responsePattern.get(responseLength - 1);
        double x_difference = (curPoint.getX() - prevPoint.getX());
        double x_sine = 1;
        // check that the difference isn't equal to zero to ensure no divide by 0 error
        if (!(x_difference == 0)) {
            // this will make x_sine +1 or -1 depending on the sine
            x_sine = x_difference / Math.abs(x_difference);
        }

        theta = Math.atan((curPoint.getY() - prevPoint.getY()) / (curPoint.getX() - prevPoint.getX()));
        d = (numExtraNormalizingPoints * deltaD) - traceDistance;

        cumulativeTime += cumulativeTime + curPoint.getTime();
        interpolated_time = cumulativeTime / normalizingPointsLength;

        for (i = numExtraNormalizingPoints; i < normalizingPointsLength; i++) {
            newX = prevPoint.getX() + (d * Math.cos(theta) * x_sine);
            newY = prevPoint.getY() + (d * Math.sin(theta) * x_sine);

            // Compute pressure and other attributes
            /* pressure */
            newPressure = curPoint.getPressure() + (((curPoint.getPressure() - prevPoint.getPressure()) / (computeEuclideanDistance(curPoint, prevPoint))) * d);
            /* distance */
            newDistance = computeEuclideanDistance(new Point(newX, newY, 0), curPoint);
            /* velocity */
            newVelocity = 1.0;
            /* acceleration */
            newAcceleration = 1.0;
            // TODO velocity and acceleration
            Point p = new Point(newX, newY);
            p.set_metric(Point.Metrics.PRESSURE, newPressure);
            p.set_metric(Point.Metrics.DISTANCE, newDistance);
            p.set_metric(Point.Metrics.TIME, interpolated_time);
            p.set_metric(Point.Metrics.VELOCITY, newVelocity);
            p.set_metric(Point.Metrics.ACCELERATION, newAcceleration);

            newNormalizedList.add(p);

            d += deltaD;
        }

        this.normalizedResponsePattern = newNormalizedList;
    }

    /**
     * Computes total euclidean distance of response pattern
     *
     * @return distance of response pattern
     */
    double computeTraceDistance() {
        double distance = 0;
        Point firstPoint, secondPoint;
        for (int i = 0; i < responsePattern.size() - 1; i++) {
            firstPoint = responsePattern.get(i);
            secondPoint = responsePattern.get(i + 1);
            distance += computeEuclideanDistance(firstPoint, secondPoint);
        }
        return distance;
    }

    /**
     * transform a response. Add x,y to every point.
     */
    private void transform_response(List<Point> response_points, double x_transform, double y_transform) {
        // add x,y to every point
        for (int i = 0; i < response_points.size(); i++) {
            Point p = response_points.get(i);
            response_points.set(i, new Point(p.getX() + x_transform, p.getY() + y_transform, p.getPressure(), p.getDistance(), p.getTime()));
        }
    }

    private double getRadius(Point p) {
        return Math.sqrt(Math.pow(p.getX(), 2) + Math.pow(p.getY(), 2));
    }

    /**
     * compute the euclidean distance between two points
     */
    private double computeEuclideanDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow((p1.getX() - p2.getX()), 2) +
                Math.pow((p1.getY() - p2.getY()), 2));
    }

}

