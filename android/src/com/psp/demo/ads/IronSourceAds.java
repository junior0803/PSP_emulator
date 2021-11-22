package com.psp.demo.ads;

import static com.psp.demo.Config.TAG;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerListener;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.psp.demo.Config;

public class IronSourceAds {
	private Activity mActivity = null;
	private FrameLayout adChoicesContainer;

	public IronSourceAds(Activity activity){
		mActivity = activity;
		startIronSourceInitTask();
	}

	//IronSource Ads Implement part

	private void startIronSourceInitTask(){
		IntegrationHelper.validateIntegration(mActivity);

		// we're using an advertisingId as the 'userId'
		String advertisingId = IronSource.getAdvertiserId(mActivity);
		// set the IronSource user id
		IronSource.setUserId(advertisingId);
		// init the IronSource SDK
		IronSource.init(mActivity, Config.firebaseData.getISApkKey());
		Log.e(TAG, "InitIronSource Create");

		//Network Connectivity Status
		IronSource.shouldTrackNetworkState(mActivity, true);

	}

	public void createInterstitial() {

		// Be sure to set a listener to each product that is being initiated
		// set client side callbacks for the offerwall
		SupersonicConfig.getConfigObj().setClientSideCallbacks(true);

		IronSource.setInterstitialListener(new InterstitialListener() {
			@Override
			public void onInterstitialAdReady() {
				// called when the interstitial is ready
				Log.d(TAG, "onInterstitialAdReady");
			}

			@Override
			public void onInterstitialAdLoadFailed(IronSourceError ironSourceError) {
				// called when the interstitial has failed to load
				// you can get the error data by accessing the IronSourceError object
				// IronSourceError.getErrorCode();
				// IronSourceError.getErrorMessage();
				Log.d(TAG, "onInterstitialAdLoadFailed" + " " + ironSourceError);
			}

			@Override
			public void onInterstitialAdOpened() {
				// called when the interstitial is shown
				Log.d(TAG, "onInterstitialAdOpened");
			}

			@Override
			public void onInterstitialAdClosed() {
				// called when the interstitial has been closed
				Log.d(TAG, "onInterstitialAdClosed");
			}

			@Override
			public void onInterstitialAdShowSucceeded() {
				// called when the interstitial has been successfully shown
				Log.d(TAG, "onInterstitialAdShowSucceeded");
			}

			@Override
			public void onInterstitialAdShowFailed(IronSourceError ironSourceError) {
				// called when the interstitial has failed to load
				// you can get the error data by accessing the IronSourceError object
				// IronSourceError.getErrorCode();
				// IronSourceError.getErrorMessage();
				Log.d(TAG, "onInterstitialAdLoadFailed" + " " + ironSourceError);
			}

			@Override
			public void onInterstitialAdClicked() {
				// called when the interstitial has been clicked
				Log.d(TAG, "onInterstitialAdClicked");
			}
		});

		IronSource.loadInterstitial();
	}


	public void show_interstitial_ad() {
		if(IronSource.isInterstitialReady()) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.e(TAG, "schduleInterstial");
					//show the interstitial
					IronSource.showInterstitial();
				}
			});
		}
		IronSource.loadInterstitial();
	}

	public void createAndLoadBanner(FrameLayout frameLayout) {
		if (frameLayout == null)
			return;
		adChoicesContainer = frameLayout;
		// choose banner size
		ISBannerSize size = ISBannerSize.BANNER;

		// instantiate IronSourceBanner object, using the IronSource.createBanner API
		IronSourceBannerLayout BannerLayout = IronSource.createBanner(mActivity, size);

		// add IronSourceBanner to your container
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT);
		adChoicesContainer.addView(BannerLayout, 0, layoutParams);

		BannerLayout.setBannerListener(new BannerListener() {
			@Override
			public void onBannerAdLoaded() {
				Log.d(TAG, "onBannerAdLoaded");
				// since banner container was "gone" by default, we need to make it visible as soon as the banner is ready
				adChoicesContainer.setVisibility(View.VISIBLE);
			}

			@Override
			public void onBannerAdLoadFailed(IronSourceError error) {
				Log.d(TAG, "onBannerAdLoadFailed" + " " + error);
			}

			@Override
			public void onBannerAdClicked() {
				Log.d(TAG, "onBannerAdClicked");
			}

			@Override
			public void onBannerAdScreenPresented() {
				Log.d(TAG, "onBannerAdScreenPresented");
			}

			@Override
			public void onBannerAdScreenDismissed() {
				Log.d(TAG, "onBannerAdScreenDismissed");
			}

			@Override
			public void onBannerAdLeftApplication() {
				Log.d(TAG, "onBannerAdLeftApplication");
			}
		});
		// load ad into the created banner
		IronSource.loadBanner(BannerLayout);
	}

	public void show_banner_ad(final boolean show) {
		if (adChoicesContainer == null) { return; }
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if (show) {
					if (!adChoicesContainer.isEnabled()) {
						adChoicesContainer.setEnabled(true);
					}
					if (adChoicesContainer.getVisibility() == View.INVISIBLE) {
						adChoicesContainer.setVisibility(View.VISIBLE);
					}
				} else {
					if (adChoicesContainer.isEnabled()) { adChoicesContainer.setEnabled(false); }
					if (adChoicesContainer.getVisibility() != View.INVISIBLE) {
						adChoicesContainer.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	public void onMainPause () {
		IronSource.onPause(mActivity);
	}

	public void onMainResume () {
		IronSource.onResume(mActivity);
	}
}
