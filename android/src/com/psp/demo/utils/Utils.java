package com.psp.demo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;


public class Utils {
	public static final String moreGameInfoUri = "aHR0cHM6Ly9hcGtyb21zLmNvbS9yb21zL2FjdGlvbi8=";

	public static final String privacyPolicyUri = "aHR0cHM6Ly9hcGtyb21zLmNvbS9wcml2YWN5LXBvbGljeQ==";

	public static final String jsonDataUrl = "aHR0cHM6Ly9nYW1lYmF0Lm9ubGluZS9qdW5pb3IyMDIxL2p1bmlvcm5ldy5qc29u"; //put your link of your json here

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

    public static String readJsonFromAssets(Context context, String filePath) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filePath);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

	public static Bitmap readBitmapFromAssets(Context context, String filePath) {
		Bitmap bitmap = null;
		try {
			InputStream is = context.getAssets().open(filePath);
			bitmap = BitmapFactory.decodeStream(is);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		return bitmap;
	}
}
