package com.psp.demo.utils;

import static com.psp.demo.Config.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.psp.demo.Config;
import com.psp.demo.FirebaseData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class GetOfflineData {
	Context context;

	public GetOfflineData(Context mContext){
		this.context = mContext;
	}

	public void loadAllData(){
		Bitmap background = null;
		Bitmap playButton = null;
		Bitmap moreButton = null;
		Bitmap policyButton = null;

		String jsonStr = Utils.readJsonFromAssets(context, "OfflineJsonData.json");
		try {
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

			background = Utils.readBitmapFromAssets(context, customBackgrd);
			playButton = Utils.readBitmapFromAssets(context, customPlayButton);
			moreButton = Utils.readBitmapFromAssets(context, customMoreButton);
			policyButton = Utils.readBitmapFromAssets(context, customPolicyButton);

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
			// runOnUiThread(new Runnable() {
			//     @Override
			//     public void run() {
			//         Toast.makeText(context,
			//                 "Json parsing error: " + e.getMessage(),
			//                 Toast.LENGTH_LONG).show();
			//     }
			// });
		}
	}
}
