package com.cognizant.gtoglass.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.cognizant.gtoglass.activity.R;
import com.cognizant.gtoglass.model.Target;
import com.cognizant.gtoglass.util.MathUtils;
import com.cognizant.gtoglass.view.Display;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.util.List;

public class TargetFinderActivity extends Activity implements
        SensorEventListener {


    private final float[] mRotationMatrix = new float[16];
    public static final String TARGET_INDEX_EXTRA = TargetFinderActivity.class
            .getName() + ".TARGET_INDEX_EXTRA";

    public static final String[] TARGET_NAMES = new String[]{
            "H1 Solutions", "H2 Solutions", "H3 Solutions", "Venture Solutions"};

    public static final int[] TARGET_ICONS = new int[]{
            R.drawable.icon_camera,
            R.drawable.icon_city,
            R.drawable.icon_incident,
            R.drawable.icon_shelter,
    };
    private TextToSpeech mSpeech;
    public static final String LOG_TAG = "GTOGlass";

    private SensorManager mSensorManager;

    private final float[] mOrientations = new float[9];

    private float mHeading;

    private static final int ARM_DISPLACEMENT_DEGREES = 6;

    private GeomagneticField mGeomagneticField;
    private float mPitch;
    private Display mDisplay;
    private GestureDetector mGestureDetector;

    private List<Target> mTargets;

    private int mTargetIndex = 0;

    public int mTargetListIndex;

    private boolean mForeground;

    public float getPitch() {
        return mPitch;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate"+mTargetListIndex);
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, (String) this.getTitle());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mDisplay = new Display(this);
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });
        mTargetListIndex = getIntent().getIntExtra(TARGET_INDEX_EXTRA, mTargetListIndex);

        mTargets = Target.TARGET_LISTS.get(mTargetListIndex);
        mDisplay.showTarget(mTargets.get(mTargetIndex));
        mGestureDetector = createGestureDetector(this);

    }

    @Override
    public void onDestroy() {
        mSpeech.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d("Gesture ", "onBackPressed");
        //Toast.makeText(getApplicationContext(), "Go Back", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.h1:
                intent = new Intent(this, OneActivity.class);
                startActivity(intent);
                return true;
            case R.id.h2:
                intent = new Intent(this, TwoActivity.class);
                startActivity(intent);
                return true;
            case R.id.h3:
                intent = new Intent(this, ThreeActivity.class);
                startActivity(intent);
                return true;
            case R.id.venture:
                intent = new Intent(this, FourActivity.class);
                startActivity(intent);
                return true;
            case R.id.camera:
                intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    private boolean showUrl() {
        Log.i(LOG_TAG, "showUrl");
        if (!mDisplay.isWebViewVisible()) {
            mDisplay.showDetailsView();
            return true;
        }
        return false;
    }

    private void gotoTarget(int targetIndex) {
        mTargetIndex = targetIndex;
        if (mTargetIndex < 5) mDisplay.showTarget(mTargets.get(mTargetIndex));
        //if(!mSpeech.isSpeaking()) mSpeech.speak(mDisplay.target.name, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    private GestureDetector createGestureDetector(final Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    Log.i(LOG_TAG, " Target" + mTargetIndex);
                    if (mTargetIndex < 5)
                        mDisplay.view.loadUrl("http://10.237.77.163:9000/socket?url=" + mDisplay.target.url + "&id=" + mTargetIndex);
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    Log.d("Gesture ", "dtap");
                    openMenu();
                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }

    private void openMenu(){
        openOptionsMenu();
    }
    private void toggleShowUrl() {
        // If showing webview, hide it.
        if (mDisplay.isWebViewVisible()) {
            mDisplay.hideDetailsView();

            // Otherwise show it.
        } else {
            showUrl();
        }
    }


    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        // Do nothing.
    }

    @Override
    protected void onResume() {
        // Log.i(LOG_TAG, "onResume");

        super.onResume();
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_UI);

        // The rotation vector sensor doesn't give us accuracy updates, so we observe the
        // magnetic field sensor solely for those.
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        mForeground = true;

    }

    @Override
    protected void onPause() {
        // Log.i(LOG_TAG, "onPause");
        mForeground = false;
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public float getHeading() {
        return mHeading;
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Get the current heading from the sensor, then notify the listeners of the
            // change.
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientations);

            // Store the pitch (used to display a message indicating that the user's head
            // angle is too steep to produce reliable results.
            mPitch = (float) Math.toDegrees(mOrientations[1]);

            // Convert the heading (which is relative to magnetic north) to one that is
            // relative to true north, using the user's current location to compute this.
            float magneticHeading = (float) Math.toDegrees(mOrientations[0]);
            mHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f)
                    - ARM_DISPLACEMENT_DEGREES;
            //Log.i(LOG_TAG, "direction  " + mHeading);
            int mod = (int) (mHeading / 36);
            if (mTargetIndex != mod) {
                Log.i(LOG_TAG, "direction  " + mod);
                gotoTarget(mod);
            }
        }

        float azimuth_angle = event.values[0];
        float pitch_angle = event.values[1];
        float roll_angle = event.values[2];
        mDisplay.setOrientation(azimuth_angle, pitch_angle, roll_angle);
    }

    private float computeTrueNorth(float heading) {
        if (mGeomagneticField != null) {
            return heading + mGeomagneticField.getDeclination();
        } else {
            return heading;
        }
    }

}