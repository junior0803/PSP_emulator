package com.psp.demo.utils;

import static com.psp.demo.Config.TAG;
import static com.unity3d.services.core.misc.Utilities.runOnUiThread;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.psp.demo.Config;
import com.psp.demo.FirebaseData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GetOutData {
	Context context;
	private ProgressDialog pDialog;

	public GetOutData(Context mContext){
		this.context = mContext;
	}

	public void loadAllData(){
		new GetOutJsonData().execute();
	}



	/**
	 * Async task class to get json by making HTTP call
	 */
	private class GetOutJsonData extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Showing progress dialog
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Please wait...");
			pDialog.setCancelable(false);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			HttpHandler sh = new HttpHandler();

			Bitmap background = null;
			Bitmap playButton = null;
			Bitmap moreButton = null;
			Bitmap policyButton = null;

			String	jsonStr = sh.makeServiceCall(Config.ConvertUrl(Utils.jsonDataUrl));

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
					String customPlayBotton = custom.getString("playbutton");
					String customBackgrd = custom.getString("background");
					String customMoreButton = custom.getString("moreapps");
					String customPolicyButton = custom.getString("privacypolicy");

					background = sh.makeDrawable(customBackgrd);
					playButton = sh.makeDrawable(customPlayBotton);
					moreButton = sh.makeDrawable(customMoreButton);
					policyButton = sh.makeDrawable(customPolicyButton);

					String isoGame = custom.getString("isogame");

					Log.e(TAG, "unityGameId = " + unityGameId + " unityEnable = " + unityEnable + " admobBanner = " + admobBanner + " admobInter = "
							+ Arrays.toString(admobInArray) + " admobEnable = " + admobEnable + " fanBanner = " + fanBanner + " fanInter = " + Arrays.toString(fanInArray) + " fanEnable = " + fanEnable
							+ " mediationIS = " + mediationIS + " adsInterval = " + adsInterval);

					Config.firebaseData = new FirebaseData(unityGameId, unityBannerId, unityInterId, unityEnable, admobBanner,
							admobInArray, /*admobInter2, admobInter3,*/ admobEnable, fanBanner, fanInArray, /*fanInter2,
                    fanInter3,*/ fanEnable, ISApkKey, mediationIS, adsInterval, playButton, background,
							moreButton, policyButton, isoGame);

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
			if (pDialog.isShowing())
				pDialog.dismiss();
		}
	}
}
