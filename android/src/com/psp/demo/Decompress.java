package com.psp.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Decompress {
	static final String LOGTAG = "junior";
	static final int BUFSIZE = 5192;
	static final String ZIP_FILTER = "assets";

	public static String getExtensionPath(Activity activity) {
		File[] listFiles;
		File assetfile = new File(Environment.getExternalStorageDirectory(), "Android/obb/" + activity.getPackageName());
		if (assetfile.exists() && (listFiles = assetfile.listFiles()) != null) {
			for (File file : listFiles) {
				String absolutePath = file.getAbsolutePath();
				if (absolutePath.endsWith(".iso")) {
					return file.getAbsolutePath();
				}
			}
		}
		return null;
	}

	public static String getGameFolder(Context context) {
		File file = new File(context.getObbDir(), "");
		if (!file.exists()) {
			file.mkdir();
		}
		return file.getAbsolutePath();
	}

	public static boolean isPackageInstalled(Activity activity, String str) {
		PackageInfo packageInfo;
		PackageManager packageManager = activity.getPackageManager();
		if (packageManager != null) {
			try {
				packageInfo = packageManager.getPackageInfo(str, 0);
			} catch (Exception unused) {
				return false;
			}
		} else {
			packageInfo = null;
		}
		return packageInfo != null;
	}


	public static String getGamePath(Context context) {
		//File file = new File(context.getFilesDir(), "data");
		File file = new File(context.getObbDir() + "");
		if (!file.exists()) {
			file.mkdir();
		}
		File[] listFiles = file.listFiles();
		if (listFiles != null) {
			for (File file2 : listFiles) {

				String absolutePath = file2.getAbsolutePath();
				if (absolutePath.endsWith(".iso")) {
					return file2.getAbsolutePath();
				}
			}
		}
		return null;
	}

	public static void showDialog(final Activity activity, String str) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		//Uncomment the below code to Set the message and title from the strings.xml file
		builder.setMessage(R.string.missing_data)
				.setTitle(R.string.app_name)
				.setMessage("Do you want to close this application ?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Toast.makeText(activity,"you choose yes action for alertbox",
								Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//  Action for 'NO' Button
						dialog.cancel();
						Toast.makeText(activity,"you choose no action for alertbox",
								Toast.LENGTH_SHORT).show();
					}
				});
		//Creating dialog box
		AlertDialog alert = builder.create();
		alert.setTitle(R.string.app_name);
		alert.show();
		builder.show();
	}


	public static void unzipFromAssets(Context context, String src, String dest, ProgressBar progressBar) {
		try {
			InputStream open = context.getAssets().open(src);
			int available = open.available();
			unzip(context, open, dest, progressBar, available);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void unzipFromUrl(Context context, String url, String dest, ProgressBar progressBar) {
		try {
			URL Url = new URL(url);
			URLConnection connection = Url.openConnection();
			connection.connect();
			int lenghtOfFile = connection.getContentLength();
			InputStream input = new BufferedInputStream(Url.openStream(), 8192);
			unzip(context, input, dest, progressBar, lenghtOfFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void unzip(Context context, InputStream inputStream, String str, ProgressBar progressBar, int length) {
		dirChecker(str, "");
		byte[] bArr = new byte[BUFSIZE];
		try {
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);
			int i = 0;
			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();
				if (nextEntry != null) {
					if (nextEntry.isDirectory()) {
						String name = nextEntry.getName();
						dirChecker(str, name);
					} else {
						File file = new File(str, "main.83." + context.getPackageName() + ".iso");
						Log.e(LOGTAG, file.getAbsolutePath());
						if (!file.exists()) {
							if (file.createNewFile()) {
								FileOutputStream fileOutputStream = new FileOutputStream(file);
								while (true) {
									int read = zipInputStream.read(bArr);
									if (read == -1) {
										break;
									}
									i += read;
									int progress = (int) ((((float) 75) * ((float) i)) / ((float) length));
									if (progressBar != null) {
										progressBar.setProgress(progress);
									}
									fileOutputStream.write(bArr, 0, read);
								}
								zipInputStream.closeEntry();
								fileOutputStream.close();
							}
						}
					}
				} else {
					zipInputStream.close();
					return;
				}
			}
		} catch (Exception e) {
			Log.e(LOGTAG, "unzip", e);
		}
	}


	private static void dirChecker(String str, String str2) {
		File file = new File(str, str2);
		if (!file.isDirectory() && !file.mkdirs()) {
			Log.w(LOGTAG, "Failed to create folder " + file.getName());
		}
	}
}
