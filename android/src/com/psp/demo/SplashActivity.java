package com.psp.demo;

import static com.psp.demo.Config.SPLASH_DISPLAY_DURATION;
import static com.psp.demo.Config.TAG;
import static com.unity3d.services.core.misc.Utilities.runOnUiThread;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.psp.demo.permission.EasyPermission;
import com.psp.demo.utils.HttpHandler;
import com.psp.demo.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class SplashActivity extends AppCompatActivity {

	public static final String INTENT_RATING = "intent_rating";
	private Context context;
	TextView textV, textAppName;
	private int countProgress = 0 ;
	private ProgressBar progressBar;
	private TextView loading;
	private final Handler handler = new Handler();
	Animation appTitleFade;
	PackageInfo infoApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		context = this;
		textV = findViewById(R.id.text_v);
		textAppName = findViewById(R.id.app_name);
		progressBar = findViewById(R.id.progressBar);
		loading = findViewById(R.id.loading);


		PackageManager manager = this.getPackageManager();
		try {
			infoApp = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
			textV.setText("V: "+infoApp.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		appTitleFade = AnimationUtils.loadAnimation(this, R.anim.textout);
		textV.setAnimation(appTitleFade);
		textAppName.setAnimation(appTitleFade);

		//Permission Check part
		if (hasPermission("android.permission.WRITE_EXTERNAL_STORAGE")){
			Log.e(TAG, "Permission Succcessfully allowed");
		} else {
			Log.e(TAG, "Permission Request");
			EasyPermission.requestPermissions(this, getString(R.string.storage_permission_message), 101, "android.permission.WRITE_EXTERNAL_STORAGE");
		}
		new GetOutJsonData().execute();
	}


	private boolean hasPermission(String str) {
		return checkSelfPermission(str) ==
				PackageManager.PERMISSION_GRANTED;
	}


	private void open(){
		Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
		mainIntent.putExtra(INTENT_RATING, true);
		Bundle bundle = new Bundle();
		mainIntent.putExtras(bundle);
		startActivity(mainIntent);
		finish();
	}

	public void setProgressBar(){
		Thread splashTread = new Thread() {
			@Override
			public void run() {
				try {
					while (countProgress < 100) {
						countProgress += 1;
						handler.post(new Runnable() {
							public void run() {
								progressBar.setProgress(countProgress);
								loading.setText("-- " + countProgress + " --");
							}
						});
						sleep(SPLASH_DISPLAY_DURATION/100);
					}
				} catch (InterruptedException e) {
					e.toString();
				}
				open();
			}

		};
		splashTread.start();
	}

	/**
	 * Async task class to get json by making HTTP call
	 */
	private class GetOutJsonData extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			super.onPreExecute();
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(0);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			
			Bitmap background = null;
			Bitmap playButton = null;
			Bitmap moreButton = null;
			Bitmap policyButton = null;

			HttpHandler sh = new HttpHandler();

			String jsonStr = "";

			if (Config.offlineDataJson)
				jsonStr = Utils.readJsonFromAssets(context, "OfflineJsonData.json");
			else if (Config.onlineFirebaseDataJson) {
				jsonStr = sh.makeServiceCall(Config.ConvertUrl(Utils.jsonDataUrl));
			}
			if (jsonStr != null) {
				try {
					Log.e(TAG, "response = " + jsonStr);
					JSONObject jsonObj = new JSONObject(jsonStr);
					JSONObject unity = jsonObj.getJSONObject("unity");

					String unityGameId = unity.getString("unityGameId");
					String unityBannerId = unity.getString("unityBannerId");
					String unityInterId = unity.getString("unityInterId");
					boolean unityEnable = unity.getBoolean("enable");

					JSONObject admob = jsonObj.getJSONObject("admob");
					String admobBanner = admob.getString("banner");

					JSONArray admobInter = admob.getJSONArray("inter");
					String [] admobInArray = new String[admobInter.length()];
					for (int i = 0; i < admobInter.length(); i ++)
						admobInArray[i] = admobInter.getString(i);

					boolean admobEnable = admob.getBoolean("enable");

					JSONObject fan = jsonObj.getJSONObject("fan");
					String fanBanner = fan.getString("banner");
					JSONArray fanInter = fan.getJSONArray("inter");

					String [] fanInArray = new String[fanInter.length()];
					for (int i = 0; i < fanInter.length(); i ++)
						fanInArray[i] = fanInter.getString(i);
					boolean fanEnable = fan.getBoolean("enable");

					JSONObject ironsource = jsonObj.getJSONObject("ironsource");
					String ISApkKey = ironsource.getString("apkKey");
					boolean mediationIS = ironsource.getBoolean("mediationIS");

					int adsInterval = jsonObj.getInt("adsInterval");

					JSONObject custom = jsonObj.getJSONObject("custom");
					String customPlayButton = custom.getString("playbutton");
					String customBackgrd = custom.getString("background");
					String customMoreButton = custom.getString("moreapps");
					String customPolicyButton = custom.getString("privacypolicy");

					if (Config.offlineDataJson){
						background = Utils.readBitmapFromAssets(context, customBackgrd);
						playButton = Utils.readBitmapFromAssets(context, customPlayButton);
						moreButton = Utils.readBitmapFromAssets(context, customMoreButton);
						policyButton = Utils.readBitmapFromAssets(context, customPolicyButton);
					} else if (Config.onlineFirebaseDataJson){
						background = sh.makeDrawable(customBackgrd);
						playButton = sh.makeDrawable(customPlayButton);
						moreButton = sh.makeDrawable(customMoreButton);
						policyButton = sh.makeDrawable(customPolicyButton);
					}

					String isoGame = custom.getString("isogame");

					Log.e(TAG, "unityGameId = " + unityGameId + " unityEnable = " + unityEnable + " admobBanner = " + admobBanner + " admobInter = "
							+ Arrays.toString(admobInArray) + " admobEnable = " + admobEnable + " fanBanner = " + fanBanner + " fanInter = " + Arrays.toString(fanInArray) + " fanEnable = " + fanEnable
							+ " mediationIS = " + mediationIS + " adsInterval = " + adsInterval + " isoGame = " + isoGame);

					Config.firebaseData = new FirebaseData(unityGameId, unityBannerId, unityInterId, unityEnable, admobBanner,
							admobInArray, /*admobInter2, admobInter3,*/ admobEnable, fanBanner, fanInArray, /*fanInter2,
                    fanInter3,*/ fanEnable, ISApkKey, mediationIS, adsInterval, playButton, background,
							moreButton, policyButton, isoGame);

					setProgressBar();

				} catch (final JSONException e) {
					Log.e(TAG, "Json parsing error: " + e.getMessage());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(context,
									"Json parsing error: " + e.getMessage(),
									Toast.LENGTH_LONG)
									.show();
						}
					});
				}
			} else {
				Log.e(TAG, "Couldn't get json from server.");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(context,
								"Couldn't get json from server. Check LogCat for possible errors!",
								Toast.LENGTH_LONG)
								.show();
					}
				});
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			// Dismiss the progress dialog
			progressBar.setProgress(100);
		}
	}
}
