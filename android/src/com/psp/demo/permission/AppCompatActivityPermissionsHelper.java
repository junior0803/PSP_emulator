package com.psp.demo.permission;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;


public class AppCompatActivityPermissionsHelper extends BaseSupportPermissionsHelper<AppCompatActivity> {
	public AppCompatActivityPermissionsHelper(AppCompatActivity appCompatActivity) {
		super(appCompatActivity);
	}

	@Override
	public FragmentManager getSupportFragmentManager() {
		return ((AppCompatActivity) getHost()).getSupportFragmentManager();
	}

	@Override
	public void directRequestPermissions(int i, String... strArr) {
		ActivityCompat.requestPermissions((Activity) getHost(), strArr, i);
	}

	@Override
	public boolean shouldShowRequestPermissionRationale(String str) {
		return ActivityCompat.shouldShowRequestPermissionRationale((Activity) getHost(), str);
	}

	@Override
	public Context getContext() {
		return (Context) getHost();
	}
}
