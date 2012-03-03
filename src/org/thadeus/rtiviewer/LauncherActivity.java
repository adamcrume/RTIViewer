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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class LauncherActivity extends Activity {
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
