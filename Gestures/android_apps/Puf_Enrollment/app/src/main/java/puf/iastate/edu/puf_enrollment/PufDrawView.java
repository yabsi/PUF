package puf.iastate.edu.puf_enrollment;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


public class PufDrawView extends View
{
    private Paint redPaint;
    private Paint greenPaint;
    private Paint bluePaint;
    private TextView mUpdateView;
    private Point[] challenge;
    private ArrayList<dataTypes.Point> response;
    private RegisterGesturesActivity mRGA;
    private long lastMotionEventTime;

    public PufDrawView(Context context, AttributeSet attr)
    {
        super(context, attr);
        init();
        mRGA = (RegisterGesturesActivity) context;
    }

    public void setUpdateView(TextView tv)
    {
        this.mUpdateView = tv;
    }

    private void init()
    {
        redPaint = new Paint();
        redPaint.setAntiAlias(true);
        redPaint.setStyle(Paint.Style.STROKE);
        redPaint.setColor(Color.RED);
        redPaint.setStrokeWidth(15);

        bluePaint = new Paint();
        bluePaint.setAntiAlias(true);
        bluePaint.setStyle(Paint.Style.STROKE);
        bluePaint.setColor(Color.BLUE);
        bluePaint.setStrokeWidth(10);

        greenPaint = new Paint();
        greenPaint.setAntiAlias(true);
        greenPaint.setStyle(Paint.Style.STROKE);
        greenPaint.setColor(Color.GREEN);
        greenPaint.setStrokeWidth(5);

        response = new ArrayList<>();
        lastMotionEventTime = 0;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(challenge.length >= 4)
        {
            Path challengeStart = new Path();
            challengeStart.moveTo(challenge[0].x, challenge[0].y);
            challengeStart.lineTo(challenge[1].x, challenge[1].y);

            Path challengeMiddle = new Path();
            challengeMiddle.moveTo(challenge[1].x, challenge[1].y);
            challengeMiddle.lineTo(challenge[2].x, challenge[2].y);

            Path challengePath = new Path();
            for(int i = 2; i < challenge.length; i++)
            {
                if(i == 2)
                {
                    challengePath.moveTo(challenge[i].x, challenge[i].y);
                }
                else
                {
                    challengePath.lineTo(challenge[i].x, challenge[i].y);
                }
            }
            canvas.drawPath(challengePath, redPaint);
            canvas.drawPath(challengeMiddle, bluePaint);
            canvas.drawPath(challengeStart, greenPaint);
        }
        else if(challenge.length == 2)
        {
            Path challengeLine = new Path();
            challengeLine.moveTo(challenge[0].x, challenge[0].y);
            challengeLine.lineTo(challenge[1].x, challenge[1].y);
            canvas.drawPath(challengeLine, redPaint);
            canvas.drawPoint(challenge[0].x, challenge[0].y, greenPaint);
        }
    }

    public void giveChallenge(Point[] challenge)
    {
        this.challenge = challenge;
        this.invalidate();
    }

    @Override
    public boolean onTouchEvent( MotionEvent me )
    {
        switch( me.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
                //mUpdateView.setText( "(x,y) = (" + Math.round(me.getX()) + ", " + Math.round(me.getY()) + ") - Pressure = " + me.getPressure() + " Time = " + (me.getEventTime()-me.getDownTime()));
                mUpdateView.setText( "Pressure = " + me.getPressure());
                response.clear();
                response.add(new dataTypes.Point(me.getX(), me.getY(), me.getPressure(), -1, (double) me.getEventTime()-me.getDownTime()));
                break;
            case MotionEvent.ACTION_UP:
                mUpdateView.setText( "No touch detected." );
                mRGA.onResponseAttempt(response);
                break;
            case MotionEvent.ACTION_MOVE:

                if(this.lastMotionEventTime == 0) {
                    this.lastMotionEventTime = me.getEventTime();
                }

                //mUpdateView.setText( "(x,y) = (" + Math.round(me.getX()) + ", " + Math.round(me.getY()) + ") - Pressure = " + me.getPressure() + " Time = " + (me.getEventTime()-this.lastMotionEventTime));
                mUpdateView.setText( "Pressure = " + me.getPressure());
                response.add(new dataTypes.Point(me.getX(), me.getY(), me.getPressure(), -1, (double) me.getEventTime()-this.lastMotionEventTime));
                this.lastMotionEventTime = me.getEventTime();

                break;
            default:
                break;
        }

        return true;
    }

    public interface ResponseListener{
        void onResponseAttempt(ArrayList<dataTypes.Point> response);
    }
}
