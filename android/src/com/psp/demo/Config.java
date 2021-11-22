package com.psp.demo;

import android.util.Base64;

import com.psp.demo.utils.GetOutData;

import java.io.UnsupportedEncodingException;

public class Config {
	public static final int SPLASH_DISPLAY_DURATION = 3000; //if you want change time of splash activity (in millisecond)

    public static final String TAG = "Psp-demo";

    //Select "true" in your favorite choice and "false" in another choices.
    public static final boolean offlineDataJson = false;  //if you want put your data in assets
    public static final boolean onlineFirebaseDataJson = true;  //if you want put your data in firebase

    // ** for put you privacy policy go to utils.Utils.PrivacyPolicy (text or html) ** //
    public static FirebaseData firebaseData;

    public static final String Admob = "admob";
    public static final String Facebook = "facebook";
    public static final String Appodeal = "appodeal";
    public static final String Unity = "unity";

	public static String ConvertUrl(String url){
		String text = "";
		byte[] data = Base64.decode(url, Base64.DEFAULT);
		try {
			text = new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return text;
	}
}
