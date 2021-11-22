package com.psp.demo.ads;

import android.app.Activity;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.view.View;

import com.ironsource.mediationsdk.C;
import com.ironsource.mediationsdk.U;
import com.psp.demo.Config;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBannerSize;
import com.unity3d.services.banners.UnityBanners;
import static com.psp.demo.Config.TAG;
import android.util.Log;
import android.widget.Toast;

public class UnitysAds<iUnityBannerListener> {

	public Activity mActivity = null;
	private IUnityAdsListener listenerInterstitialAd;
	UnityBannerListener iUnityBannerListener;
	BannerView mAdView;

	private final Boolean testMode = false;
	private String mGameId = "";
	private String mUnityBannerId = "";
	private String mUnityInterId = "";
	private FrameLayout mFrameLayout = null;


	public UnitysAds(Activity activity) {
		Log.e(TAG, "UnitysAds");
		mActivity = activity;
		mGameId = Config.firebaseData.getUnityGameId();
		mUnityBannerId = Config.firebaseData.getUnityAdBannerUnitId();
		mUnityInterId = Config.firebaseData.getUnityAdInterUnitId();

		UnityAds.initialize(mActivity, mGameId, testMode);

		iUnityBannerListener = new UnityBannerListener();

		listenerInterstitialAd = new IUnityAdsListener() {
			@Override
			public void onUnityAdsReady(String s) {
				//Toast.makeText(mActivity, "Interstitial Ad ready" , Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onUnityAdsStart(String s) {
				//Toast.makeText(mActivity, "Interstititl is playing", Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
				//Toast.makeText(mActivity, "Interstitial is Finished" , Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {
				//Toast.makeText(mActivity, unityAdsError.toString(), Toast.LENGTH_SHORT).show();
			}
		};
		UnityAds.setListener(listenerInterstitialAd);
	}

	public void createAndLoadBanner(FrameLayout frameLayout){
		Log.e(TAG, "UnitysAds");
		if (UnityAds.isInitialized()){
			Log.e(TAG, "UnitysAds");
			UnityBanners.loadBanner(mActivity, mUnityBannerId);
		} else {
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							UnityBanners.loadBanner(mActivity, mUnityBannerId);
						}
					},5000);
				}
			},5000);
		}

		mAdView = new BannerView(mActivity, mUnityBannerId, new UnityBannerSize(320, 50));
		// Set the listener for banner lifcycle events:
		mAdView.setListener(iUnityBannerListener);
		frameLayout.addView(mAdView);
		mAdView.load();
	}


	private class UnityBannerListener implements BannerView.IListener {
		@Override
		public void onBannerLoaded(BannerView bannerAdView) {
			// Called when the banner is loaded.
		}

		@Override
		public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
			Log.d("SupportTest", "Banner Error" + errorInfo);
			// Note that the BannerErrorInfo object can indicate a no fill (see API documentation).
		}

		@Override
		public void onBannerClick(BannerView bannerAdView) {
			// Called when a banner is clicked.
		}


		@Override
		public void onBannerLeftApplication(BannerView bannerAdView) {
			// Called when the banner links out of the application.
		}
	}

	public void createInterstitial() {
		UnityAds.setDebugMode(false);
		UnityAds.addListener(new IUnityAdsListener() {
			@Override
			public void onUnityAdsReady(String placementId) {

			}

			@Override
			public void onUnityAdsStart(String placementId) {

			}

			@Override
			public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {

			}

			@Override
			public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {

			}
		});

	}


	public void show_interstitial_ad () {
		if (UnityAds.isReady(mUnityInterId)){
			UnityAds.show(mActivity, mUnityInterId);
		}
	}
}
