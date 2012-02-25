package org.thadeus.coin;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

public class CoinActivity extends Activity {
	private CoinSurfaceView mGLView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.main);

		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity
		mGLView = new CoinSurfaceView(this);
		setContentView(mGLView);

		if(false) {
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		Sensor sensor = sensors.get(0);
		sensorManager.registerListener(new SensorEventListener() {
			public void onSensorChanged(SensorEvent event) {
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				// TODO: Handle rotation automatically
				mGLView.updatePosition(-y, x, z);
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// ignore
			}
		}, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// The following call pauses the rendering thread.
		// If your OpenGL application is memory intensive,
		// you should consider de-allocating objects that
		// consume significant memory here.
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The following call resumes a paused rendering thread.
		// If you de-allocated graphic objects for onPause()
		// this is a good place to re-allocate them.
		mGLView.onResume();
	}
}
