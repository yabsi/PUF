package analysis;

import java.util.List;

import dataTypes.Challenge;
import dataTypes.Point;
import dataTypes.Response;
import dataTypes.UserDevicePair;

/**
 * This class will run one test. A test constitutes authenticating one response
 * against a set of responses.
 * 
 * This class will authenticate response against response_set
 * 
 * @author element
 *
 */
public class Test {
    public boolean authentication_result;
    public boolean expected_authentication_result;

    // test parameters which may be varied.
    public double pressure_allowed_deviations;
    public double distance_allowed_deviations;
    public double time_allowed_deviations;
    public double authentication_threshold;
    public double time_length_allowed_deviations;

    /**
     * this class will authenticate response against response_set
     * 
     * @param response
     * @param response_set
     */
    public Test(Response response, List<Response> response_set, boolean expected_result, List<Point> challenge_points) {
	this(response, response_set, expected_result, challenge_points,
		UserDevicePair.PRESSURE_DEFAULT_ALLOWED_DEVIATIONS, UserDevicePair.DISTANCE_DEFAULT_ALLOWED_DEVIATIONS,
		UserDevicePair.TIME_DEFAULT_ALLOWED_DEVIATIONS, UserDevicePair.TIME_LENGTH_DEFAULT_ALLOWED_DEVIATIONS,
		UserDevicePair.DEFAULT_AUTHENTICATION_THRESHOLD);
    }

    public Test(Response response, List<Response> response_set, boolean expected_result, List<Point> challenge_points,
	    Combination c) {
	this(response, response_set, expected_result, challenge_points, c.pressure_allowed_deviations,
		c.distance_allowed_deviations, c.time_allowed_deviations, c.time_length_allowed_deviations,
		c.authentication_threshold);
    }

    /**
     * final constructor which will cause the test to actually be run.
     */
    public Test(Response response, List<Response> response_set, boolean expected_result, List<Point> challenge_points,
	    double pressure_allowed_deviations, double distance_allowed_deviations, double time_allowed_deviations,
	    double time_length_allowed_deviations, double authentication_threshold) {
	this.expected_authentication_result = expected_result;
	this.pressure_allowed_deviations = pressure_allowed_deviations;
	this.distance_allowed_deviations = distance_allowed_deviations;
	this.time_allowed_deviations = time_allowed_deviations;
	this.authentication_threshold = authentication_threshold;
	this.time_length_allowed_deviations = time_length_allowed_deviations;

	// run the test
	Challenge challenge = new Challenge(challenge_points, 0);

	// add all responses to the challenge
	for (Response r : response_set) {
	    challenge.addResponse(r);
	}

	// preform the authentication
	UserDevicePair ud_pair = new UserDevicePair(0, this.pressure_allowed_deviations,
		this.distance_allowed_deviations, this.time_allowed_deviations, this.authentication_threshold);
	ud_pair.setStandardDeviations(UserDevicePair.RatioType.TIME_LENGTH, this.time_length_allowed_deviations);
	ud_pair.addChallenge(challenge);

	// set authentication result based on the outcome
	this.authentication_result = ud_pair.authenticate(response.getResponse(), challenge.getProfile());
    }
}