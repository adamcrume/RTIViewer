package org.thadeus.rtiviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class LauncherActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}


	public void showExample(View button) {
		Uri uri = Uri.parse(getClass().getResource("coin.rti").toString());
		startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), ViewActivity.class));
	}
}
