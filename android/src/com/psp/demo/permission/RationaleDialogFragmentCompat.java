package com.psp.demo.permission;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;

public class RationaleDialogFragmentCompat extends AppCompatDialogFragment {
	public static final String TAG = "RationaleDialogFragmentCompat";
	private EasyPermission.PermissionCallbacks mPermissionCallbacks;
	private EasyPermission.RationaleCallbacks mRationaleCallbacks;

	public static RationaleDialogFragmentCompat newInstance(String str, String str2, String str3, int i, int i2, String[] strArr) {
		RationaleDialogFragmentCompat rationaleDialogFragmentCompat = new RationaleDialogFragmentCompat();
		rationaleDialogFragmentCompat.setArguments(new RationaleDialogConfig(str2, str3, str, i, i2, strArr).toBundle());
		return rationaleDialogFragmentCompat;
	}

	public void showAllowingStateLoss(FragmentManager fragmentManager, String str) {
		if (!fragmentManager.isStateSaved()) {
			show(fragmentManager, str);
		}
	}

	@Override // androidx.fragment.app.Fragment, androidx.fragment.app.DialogFragment
	public void onAttach(Context context) {
		super.onAttach(context);
		if (getParentFragment() != null) {
			if (getParentFragment() instanceof EasyPermission.PermissionCallbacks) {
				this.mPermissionCallbacks = (EasyPermission.PermissionCallbacks) getParentFragment();
			}
			if (getParentFragment() instanceof EasyPermission.RationaleCallbacks) {
				this.mRationaleCallbacks = (EasyPermission.RationaleCallbacks) getParentFragment();
			}
		}
		if (context instanceof EasyPermission.PermissionCallbacks) {
			this.mPermissionCallbacks = (EasyPermission.PermissionCallbacks) context;
		}
		if (context instanceof EasyPermission.RationaleCallbacks) {
			this.mRationaleCallbacks = (EasyPermission.RationaleCallbacks) context;
		}
	}

	@Override // androidx.fragment.app.Fragment, androidx.fragment.app.DialogFragment
	public void onDetach() {
		super.onDetach();
		this.mPermissionCallbacks = null;
		this.mRationaleCallbacks = null;
	}

	@Override // androidx.appcompat.app.AppCompatDialogFragment, androidx.fragment.app.DialogFragment
	public Dialog onCreateDialog(Bundle bundle) {
		setCancelable(false);
		RationaleDialogConfig rationaleDialogConfig = new RationaleDialogConfig(getArguments());
		return rationaleDialogConfig.createSupportDialog(getContext(), new RationaleDialogClickListener(this, rationaleDialogConfig, this.mPermissionCallbacks, this.mRationaleCallbacks));
	}

}
