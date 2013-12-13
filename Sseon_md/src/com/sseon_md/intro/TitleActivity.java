package com.sseon_md.intro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.sseon_md.R;
import com.sseon_md.gui.main.MainActivity;

public class TitleActivity extends Activity {

	Handler h;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_title);
		
		h = new Handler();
		h.postDelayed(r, 1000);
	}
	
	Runnable r = new Runnable() {
		@Override
		public void run() {
			Intent i = new Intent(TitleActivity.this, MainActivity.class);
			startActivity(i);
			finish();
			
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	};
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		h.removeCallbacks(r);
	}

}
