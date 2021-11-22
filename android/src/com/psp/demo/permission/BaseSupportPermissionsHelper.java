package com.psp.demo.permission;

import androidx.fragment.app.FragmentManager;

import android.util.Log;
import androidx.fragment.app.FragmentManager;



public abstract class BaseSupportPermissionsHelper<T> extends PermissionHelper<T> {
	private static final String TAG = "BSPermissionsHelper";

	public abstract FragmentManager getSupportFragmentManager();

	public BaseSupportPermissionsHelper(T t) {
		super(t);
	}

	@Override // pub.devrel.easypermissions.helper.PermissionHelper
	public void showRequestPermissionRationale(String str, String str2, String str3, int i, int i2, String... strArr) {
		FragmentManager supportFragmentManager = getSupportFragmentManager();
		if (supportFragmentManager.findFragmentByTag(RationaleDialogFragmentCompat.TAG) != null) {
			Log.d(TAG, "Found existing fragment, not showing rationale.");
		} else {
			RationaleDialogFragmentCompat.newInstance(str, str2, str3, i, i2, strArr).showAllowingStateLoss(supportFragmentManager, RationaleDialogFragmentCompat.TAG);
		}
	}
}
