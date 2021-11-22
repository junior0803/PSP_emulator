package com.psp.demo;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.psp.demo.Config.TAG;
import static com.unity3d.services.core.misc.Utilities.runOnUiThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.ppsspp.ppsspp.PpssppActivity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.BannerListener;
import com.psp.demo.ads.AdmobAds;
import com.psp.demo.ads.FacebookAds;
import com.psp.demo.ads.IronSourceAds;
import com.psp.demo.ads.UnitysAds;
import com.psp.demo.utils.GetOfflineData;
import com.psp.demo.utils.GetOutData;
import com.psp.demo.utils.HttpHandler;
import com.psp.demo.utils.Utils;

public class MainActivity extends AppCompatActivity {
	private static final int PERMISSION_REQUEST_CODE = 2296;
	Animation animation;
	FrameLayout imageView;
	Button playButton;
	Button moreButton;
	Button policyButton;

	ProgressBar progressBar;
	TextView progressText;
	//IronSource
	AdmobAds admobAd;
	FacebookAds facebookAd;
	UnitysAds unityAd;
	IronSourceAds ironSourceAds;
	private ProgressDialog pDialog;
	private Context mContext;
	private Placement mPlacement;

	private FrameLayout mBannerParentLayout;
	private IronSourceBannerLayout mIronSourceBannerLayout;

	private boolean online = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_main);

		// Background Image Customize
		imageView = (FrameLayout)findViewById(R.id.adsLayout);

		Drawable background = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomBackground());
		imageView.setBackground(background);

		animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

		// playbutton Customize
		playButton = findViewById(R.id.play_button);

		Drawable playbutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomPlayButton());
		playButton.setBackground(playbutton);

		//moreButton Customize
		moreButton = findViewById(R.id.moregame_button);

		Drawable morebutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomMoreButton());
		moreButton.setBackground(morebutton);
		//policyButton Customzie
		policyButton = findViewById(R.id.policy_button);
		Drawable policybutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomPolicyButton());
		policyButton.setBackground(policybutton);

		progressBar = findViewById(R.id.progress);
		progressText = findViewById(R.id.progressText);

		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		imageView.startAnimation(animation);
		playButton.startAnimation(animation);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startGame();
			}
		});

		moreButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.ConvertUrl(Utils.moreGameInfoUri)));
				startActivity(browserIntent);
			}
		});

		policyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.ConvertUrl(Utils.privacyPolicyUri)));
				startActivity(browserIntent);
			}
		});
		//readData();

		if (Config.firebaseData.getAdmobEnable())
			admobAd = new AdmobAds(MainActivity.this);
		if (Config.firebaseData.getFacebookEnable())
			facebookAd = new FacebookAds(MainActivity.this);
		if (Config.firebaseData.getUnityEnable())
			unityAd = new UnitysAds(MainActivity.this);
		if (Config.firebaseData.getMediationIS())
			ironSourceAds = new IronSourceAds(MainActivity.this);

		mBannerParentLayout = (FrameLayout) findViewById(R.id.banner_footer);

		createBannerAds();
		createInterstitialAds();
	}

	private void readData() {
		if (Config.offlineDataJson){
			new GetOfflineData(this).loadAllData();
		}else if (Config.onlineFirebaseDataJson){
			new GetOutData(this).loadAllData();
		}
	}

	public final void startGame() {
		String gamePath = Decompress.getExtensionPath(this);
		Log.e(TAG, "gamPath = " + gamePath);
		if (gamePath == null || gamePath.length() == 0) {
			makeGameData();
		} else {
			openGame(gamePath);
		}
	}

	private void openGame(String gamePath) {
		Intent intent = new Intent();
		intent.setClass(this, PpssppActivity.class);
		startActivity(intent);
		finish();
	}

	private void makeGameData(){

		Log.e(TAG, "makeGameData()");

		String[] list = new String[0];
		try {
			list = getAssets().list("data");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String dataStr = null;
		if (list != null) {
			int length = list.length;
			int i = 0;
			while (true) {
				if (i >= length) {
					break;
				}
				String str = list[i];
				if (str.endsWith(".zip")) {
					dataStr = str;
					break;
				}
				i++;
			}
		}

		if (dataStr == null){
			if (Config.offlineDataJson){
				Toast.makeText(getApplicationContext(), R.string.data_failed, Toast.LENGTH_LONG).show();
				return;
			} else {
				dataStr = Config.firebaseData.getIsoGame();
				online = true;
			}
		}

		Log.e(TAG, "dataStr = " + dataStr);
		unzip(dataStr);
	}

	private void unzip(String dataStr) {
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setMax(100);
		new unzipThread().execute(dataStr);
	}

	private class unzipThread extends AsyncTask<String, Integer, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(0);
			progressText.setVisibility(View.VISIBLE);
			progressText.setText(R.string.loding);
			playButton.setEnabled(false);
		}

		@Override
		protected String doInBackground(String... strings) {
			if (online){
				Decompress.unzipFromUrl(getApplicationContext(), strings[0],
						Decompress.getGameFolder(getApplicationContext()), progressBar);
			} else {
				Decompress.unzipFromAssets(getApplicationContext(), "data/" + strings[0],
						Decompress.getGameFolder(getApplicationContext()), progressBar);
			}
			return strings[0];
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			progressBar.setProgress(100);
			progressBar.setVisibility(View.INVISIBLE);
			progressText.setVisibility(View.VISIBLE);
			progressText.setText(R.string.successload);
			playButton.setEnabled(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Config.firebaseData != null){
			if (ironSourceAds != null){
				ironSourceAds.onMainResume();
			}
			if (admobAd != null){
				admobAd.onMainResume();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// call the IronSource onPause method
		if (Config.firebaseData != null){
			if (ironSourceAds != null){
				ironSourceAds.onMainPause();
			}
			if (admobAd != null){
				admobAd.onMainPause();
			}
		}
	}

	/**
	 * Destroys IronSource Banner and removes it from the container
	 *
	 */
	private void destroyAndDetachBanner() {
		IronSource.destroyBanner(mIronSourceBannerLayout);
		if (mBannerParentLayout != null) {
			mBannerParentLayout.removeView(mIronSourceBannerLayout);
		}
	}

	//Banner : Start
	public void createBannerAds(){
		Log.e(TAG, "createBannerAds()");
		if (Config.firebaseData != null){
			if (Config.firebaseData.getMediationIS()){
				ironSourceAds.createAndLoadBanner(mBannerParentLayout);
				ironSourceAds.show_banner_ad(true);
			}
			if (Config.firebaseData.getAdmobEnable()){
				admobAd.createAndLoadBanner(mBannerParentLayout);
				admobAd.show_banner_ad(true);
			}
			if (Config.firebaseData.getFacebookEnable()){
				facebookAd.createAndLoadBanner(mBannerParentLayout);
				facebookAd.show_banner_ad(true);
			}
			if (Config.firebaseData.getUnityEnable()){
				unityAd.createAndLoadBanner(mBannerParentLayout);
			}
		}
	}
	//Banner : End

	//InterstitialAd : Start
	public void createInterstitialAds(){
		Log.e(TAG, "createInterstitialAds()");
		if (Config.firebaseData != null){
			if (Config.firebaseData.getMediationIS()){
				ironSourceAds.createInterstitial();
			}
			if (Config.firebaseData.getAdmobEnable()){
				admobAd.createInterstitial(0);
			}
			if (Config.firebaseData.getFacebookEnable()){
				facebookAd.createInterstitial(0);
			}
			if (Config.firebaseData.getUnityEnable()) {
				unityAd.createInterstitial();
			}
		}
	}

	//write storage permission
	private boolean checkPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager();
		} else {
			int result = ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE);
			int result1 = ContextCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE);
			return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
		}
	}

	private void requestPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			try {
				Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				intent.addCategory("android.intent.category.DEFAULT");
				intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
				startActivityForResult(intent, 2296);
			} catch (Exception e) {
				Intent intent = new Intent();
				intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
				startActivityForResult(intent, 2296);
			}
		} else {
			//below android 11
			ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 2296) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				if (Environment.isExternalStorageManager()) {
					// perform action when allow permission success
					Toast.makeText(this, "write storage permission allowed!", Toast.LENGTH_SHORT).show();

				} else {
					Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0) {
				boolean READ_EXTERNAL_STORAGE = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				boolean WRITE_EXTERNAL_STORAGE = grantResults[1] == PackageManager.PERMISSION_GRANTED;

				if (READ_EXTERNAL_STORAGE && WRITE_EXTERNAL_STORAGE) {
					// perform action when allow permission success
					Toast.makeText(this, "write storage permission allowed!", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}
