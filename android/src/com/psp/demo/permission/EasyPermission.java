package com.psp.demo.permission;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EasyPermission {

	private static final String TAG = "EasyPermissions";


	public interface PermissionCallbacks extends ActivityCompat.OnRequestPermissionsResultCallback {
		void onPermissionsDenied(int i, List<String> list);

		void onPermissionsGranted(int i, List<String> list);
	}

	public interface RationaleCallbacks {
		void onRationaleAccepted(int i);

		void onRationaleDenied(int i);
	}

	public static boolean hasPermissions(Context context, String... strArr) {
		if (context != null) {
			for (String str : strArr) {
				if (ContextCompat.checkSelfPermission(context, str) != 0) {
					return false;
				}
			}
			return true;
		} else {
			throw new IllegalArgumentException("Can't check permissions for null context");
		}
	}


	public static void requestPermissions(Activity activity, String str, int i, String... strArr) {
		requestPermissions(new PermissionRequest.Builder(activity, i, strArr).setRationale(str).build());
	}

	public static void requestPermissions(Fragment fragment, String str, int i, String... strArr) {
		requestPermissions(new PermissionRequest.Builder(fragment, i, strArr).setRationale(str).build());
	}

	public static void requestPermissions(PermissionRequest permissionRequest) {
		if (hasPermissions(permissionRequest.getHelper().getContext(), permissionRequest.getPerms())) {
			notifyAlreadyHasPermissions(permissionRequest.getHelper().getHost(), permissionRequest.getRequestCode(), permissionRequest.getPerms());
		} else {
			permissionRequest.getHelper().requestPermissions(permissionRequest.getRationale(), permissionRequest.getPositiveButtonText(), permissionRequest.getNegativeButtonText(), permissionRequest.getTheme(), permissionRequest.getRequestCode(), permissionRequest.getPerms());
		}
	}

	private static void notifyAlreadyHasPermissions(Object obj, int i, String[] strArr) {
		int[] iArr = new int[strArr.length];
		for (int i2 = 0; i2 < strArr.length; i2++) {
			iArr[i2] = 0;
		}
		onRequestPermissionsResult(i, strArr, iArr, obj);
	}


	public static void onRequestPermissionsResult(int i, String[] strArr, int[] iArr, Object... objArr) {
		ArrayList arrayList = new ArrayList();
		ArrayList arrayList2 = new ArrayList();
		for (int i2 = 0; i2 < strArr.length; i2++) {
			String str = strArr[i2];
			if (iArr[i2] == 0) {
				arrayList.add(str);
			} else {
				arrayList2.add(str);
			}
		}
		for (Object obj : objArr) {
			if (!arrayList.isEmpty() && (obj instanceof PermissionCallbacks)) {
				((PermissionCallbacks) obj).onPermissionsGranted(i, arrayList);
			}
			if (!arrayList2.isEmpty() && (obj instanceof PermissionCallbacks)) {
				((PermissionCallbacks) obj).onPermissionsDenied(i, arrayList2);
			}
			if (!arrayList.isEmpty() && arrayList2.isEmpty()) {
				runAnnotatedMethods(obj, i);
			}
		}
	}

	private static void runAnnotatedMethods(Object obj, int i) {
		Class<?> cls = obj.getClass();
		if (isUsingAndroidAnnotations(obj)) {
			cls = cls.getSuperclass();
		}
		while (cls != null) {
			Method[] declaredMethods = cls.getDeclaredMethods();
			for (Method method : declaredMethods) {
				AfterPermissionGranted afterPermissionGranted = (AfterPermissionGranted) method.getAnnotation(AfterPermissionGranted.class);
				if (afterPermissionGranted != null && afterPermissionGranted.value() == i) {
					if (method.getParameterTypes().length <= 0) {
						try {
							if (!method.isAccessible()) {
								method.setAccessible(true);
							}
							method.invoke(obj);
						} catch (IllegalAccessException e) {
							Log.e(TAG, "runDefaultMethod:IllegalAccessException", e);
						} catch (InvocationTargetException e2) {
							Log.e(TAG, "runDefaultMethod:InvocationTargetException", e2);
						}
					} else {
						throw new RuntimeException("Cannot execute method " + method.getName() + " because it is non-void method and/or has input parameters.");
					}
				}
			}
			cls = cls.getSuperclass();
		}
	}

	private static boolean isUsingAndroidAnnotations(Object obj) {
		if (!obj.getClass().getSimpleName().endsWith("_")) {
			return false;
		}
		try {
			return Class.forName("org.androidannotations.api.view.HasViews").isInstance(obj);
		} catch (ClassNotFoundException unused) {
			return false;
		}
	}

}
