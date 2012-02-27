package org.thadeus.rtiviewer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;

public class ViewActivity extends Activity {
	private static Map<String, WeakReference<RTI>> cachedRTI = new HashMap<String, WeakReference<RTI>>();
	private RTISurfaceView mGLView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String uri1 = getIntent().getDataString();
		RTI rti;
		WeakReference<RTI> rtiRef = cachedRTI.get(uri1);
		rti = rtiRef == null ? null : rtiRef.get();
		if (rti == null) {
			rti = new RTI();
			try {
				rti.loadHSH(uri1);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			cachedRTI.put(uri1, new WeakReference<RTI>(rti));
		}

		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity
		mGLView = new RTISurfaceView(this, rti);
		setContentView(mGLView);
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
