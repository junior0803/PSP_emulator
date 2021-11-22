package com.psp.demo.permission;


import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


public class SupportFragmentPermissionHelper extends BaseSupportPermissionsHelper<Fragment>{
	public SupportFragmentPermissionHelper(Fragment fragment) {
		super(fragment);
	}

	@Override
	public FragmentManager getSupportFragmentManager() {
		return ((Fragment) getHost()).getChildFragmentManager();
	}

	@Override
	public void directRequestPermissions(int i, String... strArr) {
		((Fragment) getHost()).requestPermissions(strArr, i);
	}

	@Override
	public boolean shouldShowRequestPermissionRationale(String str) {
		return ((Fragment) getHost()).shouldShowRequestPermissionRationale(str);
	}

	@Override
	public Context getContext() {
		return ((Fragment) getHost()).getActivity();
	}

}
