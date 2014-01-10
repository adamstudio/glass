package com.cognizant.gtoglass.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.cognizant.gtoglass.R;
import com.cognizant.gtoglass.http.FindChpIncidentsCall;
import com.cognizant.gtoglass.http.FindChpIncidentsCall.OnFindChpIncidentsListener;
import com.cognizant.gtoglass.http.FindRequestData;
import com.cognizant.gtoglass.http.FindSheltersCall;
import com.cognizant.gtoglass.http.FindSheltersCall.OnFindSheltersListener;
import com.cognizant.gtoglass.http.Placemark;
import com.cognizant.gtoglass.model.Target;
import com.cognizant.gtoglass.model.TargetCities;
import com.cognizant.gtoglass.util.MathUtils;
import com.cognizant.gtoglass.view.Display;

import java.util.LinkedList;
import java.util.List;

public class TargetFinderActivity extends Activity implements
		SensorEventListener, LocationListener, OnFindChpIncidentsListener,
		OnFindSheltersListener {

	// TODO use wake lock to turn on screen when run

	// TODO show distance under icon?

	// TODO sort targets by closest target

	// TODO option to pick other targets: shelter vs. camera, etc.

    private final float[] mRotationMatrix = new float[16];
	public static final String TARGET_INDEX_EXTRA = TargetFinderActivity.class
			.getName() + ".TARGET_INDEX_EXTRA";

	public static final String[] TARGET_NAMES = new String[] {
			"H1 Solutions", "H2 Solutions", "H3 Solutions", "Venture Solutions" };
	
	public static final int[] TARGET_ICONS = new int[] {
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
	private Sensor mOrientation;
    private float mPitch;
	private LocationManager mLocationManager;

	private Display mDisplay;

	private List<Target> mTargets;

	private int mTargetIndex;

	public int mTargetListIndex;

	private boolean mForeground;

    public float getPitch() {
        return mPitch;
    }

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, (String) this.getTitle());

		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// TODO supposed to be more accurate to compose compass and
		// accelerometer yourself
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mDisplay = new Display(this);
        mSpeech= new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });
		// TODO sort nearest first
		// TODO add cameras, shelters, etc..

		mTargetListIndex = getIntent().getIntExtra(TARGET_INDEX_EXTRA, 0);
		if (mTargetListIndex <= 1) {
			mTargets = Target.TARGET_LISTS.get(mTargetListIndex);
			mDisplay.showTarget(mTargets.get(mTargetIndex));
		}

		// XXX use default location as Palo Alto for now.
		// This gets overridden by any last used location in onResume,
		// and by any fresh locations.
		mDisplay.setLocation(TargetCities.PALO_ALTO.asLocation());

	}

    @Override
    public void onDestroy() {
        mSpeech.shutdown();
        super.onDestroy();
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.i(LOG_TAG, "onTouchEvent, event = " + event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		Log.i(LOG_TAG, "dispatchTouchEvent, event = " + ev);

		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			toggleShowUrl();
			return true;
		}

		return super.dispatchTouchEvent(ev);
	}

	private boolean showUrl() {
		Log.i(LOG_TAG, "showUrl");

		if (!mDisplay.isWebViewVisible()) {
			mDisplay.showDetailsView();
			return true;
		}
		return false;
	}

	private void nextTarget() {
		Log.i(LOG_TAG, "nextTarget");

		mTargetIndex++;
		if (mTargetIndex >= mTargets.size()) {
			mTargetIndex = 0;
		}
		mDisplay.showTarget(mTargets.get(mTargetIndex));
	}

    private void gotoTarget(float targetIndex) {
        Log.i(LOG_TAG, "gotoTarget");
        mTargetIndex = (int) targetIndex;
        mDisplay.showTarget(mTargets.get(mTargetIndex));
        //mSpeech.speak(mDisplay.target.name, TextToSpeech.QUEUE_ADD, null);

    }


    private void previousTarget() {
		Log.i(LOG_TAG, "previousTarget");

		mTargetIndex--;
		if (mTargetIndex < 0) {
			mTargetIndex = mTargets.size() - 1;
		}
		mDisplay.showTarget(mTargets.get(mTargetIndex));
	}

	@Override
	public boolean dispatchKeyEvent(final KeyEvent event) {
		Log.i(LOG_TAG, "dispatchKeyEvent, event = " + event);

		final int action = event.getAction();
		if (action != KeyEvent.ACTION_DOWN) {
			return false;
		}

		final int keyCode = event.getKeyCode();
		switch (keyCode) {
		// Back button on standard Android, swipe down on Google Glass
		case KeyEvent.KEYCODE_BACK:
			// If showing main AR screen on down swipe, leave program.
			if (!mDisplay.isWebViewVisible()) {
				finish();
				// If showing a camera, go back to AR screen.
			} else {
				mDisplay.hideDetailsView();
			}
			return true;

			// Left and right swipe through the cameras on Google Glass.
			// On phone, volume keys move through cameras
		case KeyEvent.KEYCODE_TAB:
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (event.isShiftPressed()) {
				previousTarget();
			} else {
				nextTarget();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			previousTarget();
			return true;

			// Tapping views the camera.
		case KeyEvent.KEYCODE_DPAD_CENTER:
			toggleShowUrl();
			return true;

		default:
			return super.dispatchKeyEvent(event);
		}
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

        mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_NORMAL);
		final List<String> providers = mLocationManager.getAllProviders();
		for (String provider : providers) {
			// Set last known location if we have it.
			// TODO indicate to user if very out of date?
			// Time label in corner? some sort of scanner ping lines moving
			// outward?
			final Location lastKnownLocation = mLocationManager
					.getLastKnownLocation(provider);
			mDisplay.setLocation(lastKnownLocation);

			final boolean enabled = mLocationManager
					.isProviderEnabled(provider);
			Log.i(LOG_TAG, "Found provider: " + provider + ", enabled = "
					+ enabled);
			mLocationManager.requestLocationUpdates(provider, 0, 0, this);
		}

		mForeground = true;
		loadChpIfNeeded();
	}

	private void loadChpIfNeeded() {
		Log.i(LOG_TAG, "loadChpIfNeeded");

		// TODO check if have gotten a location yet?

		if (2 == mTargetListIndex) {

			final Location location = TargetCities.PALO_ALTO.asLocation();

			FindRequestData request = new FindRequestData(
					location.getLatitude(), location.getLongitude());

			new FindChpIncidentsCall(this, this, request).downloadIncidents();
			return;
		}

		if (3 == mTargetListIndex) {

			final Location location = TargetCities.PALO_ALTO.asLocation();

			FindRequestData request = new FindRequestData(
					location.getLatitude(), location.getLongitude());

			new FindSheltersCall(this, this, request).downloadShelters();
		}
	}

	@Override
	protected void onPause() {
		// Log.i(LOG_TAG, "onPause");
		mForeground = false;
		super.onPause();
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
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
            Log.i(LOG_TAG, "direction  " + mHeading);
            float mod =(mHeading/10);
            gotoTarget( mod);
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

	@Override
	public void onLocationChanged(final Location location) {
		// Log.i(LOG_TAG, "onLocationChanged");

		mDisplay.setLocation(location);
	}

	@Override
	public void onProviderDisabled(final String provider) {
		// Log.i(LOG_TAG, "onProviderDisabled");
		// TODO warn user if no providers enabled
	}

	@Override
	public void onProviderEnabled(final String provider) {
		// Log.i(LOG_TAG, "onProviderEnabled");
	}

	@Override
	public void onStatusChanged(final String provider, final int status,
			final Bundle extras) {
		// Log.i(LOG_TAG, "onStatusChanged");
	}

	public static double microDegreesToDegrees(int microDegrees) {
		return microDegrees / 1E6;
	}

	@Override
	public void onFindChpIncidents(List<Placemark> data) {
		Log.i(LOG_TAG, "onFindChpIncidents");

		if (!mForeground) {
			return;
		}

		List<Target> targets = new LinkedList<Target>();
		for (Placemark p : data) {

			double lat = microDegreesToDegrees(p.getLat());
			double lon = microDegreesToDegrees(p.getLon());
			Target target = new Target(null, lon, lat, p.getName());
			target.description = p.getDescription();
			target.indicatorDrawableId = R.drawable.marker_incident;
			targets.add(target);
		}

		mTargets = targets;
		mDisplay.showTarget(mTargets.get(mTargetIndex));
	}

	@Override
	public void onFindShelters(List<Placemark> data) {
		Log.i(LOG_TAG, "onFindShelters");

		if (!mForeground) {
			return;
		}

		List<Target> targets = new LinkedList<Target>();
		for (Placemark p : data) {

			double lat = microDegreesToDegrees(p.getLat());
			double lon = microDegreesToDegrees(p.getLon());
			Target target = new Target(null, lon, lat, p.getName());
			target.description = p.getDescription();
			target.indicatorDrawableId = R.drawable.marker_shelter;
			targets.add(target);
		}

		mTargets = targets;
		mDisplay.showTarget(mTargets.get(mTargetIndex));
	}
    @Override
    protected void onStop() {
        super.onStop();
    }

}
