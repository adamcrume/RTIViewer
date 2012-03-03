/*
    Copyright 2012 Adam Crume

    This file is part of RTIViewer.

    RTIViewer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    RTIViewer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RTIViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

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
