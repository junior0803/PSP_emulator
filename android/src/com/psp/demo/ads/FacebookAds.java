package com.psp.demo.ads;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.psp.demo.Config;
import static com.psp.demo.Config.TAG;

public class FacebookAds {
	public Activity mActivity = null;
	private AdView mAdView = null;
	private AdListener adListener;
	private InterstitialAd interstitialAd;
	private InterstitialAdListener interstitialAdListener;
	private FrameLayout adChoicesContainer;
	public boolean adInterstitialAdLoaded = false;

	public FacebookAds(Activity activity) {
		mActivity = activity;
	}

	public void createAndLoadBanner(FrameLayout frameLayout){
		adChoicesContainer = frameLayout;
		mAdView = new AdView(mActivity, Config.firebaseData.getFacebookBannerUnitId(), AdSize.BANNER_HEIGHT_50);

		adListener = new AdListener() {
			@Override
			public void onError(Ad ad, AdError adError) {
				Log.e(TAG, "Ad 50 Error: " + adError.getErrorMessage());
			}

			@Override
			public void onAdLoaded(Ad ad) {
				adChoicesContainer.setVisibility(View.VISIBLE);
				Log.e(TAG, "Ad Loaded");
			}

			@Override
			public void onAdClicked(Ad ad) {
			}

			@Override
			public void onLoggingImpression(Ad ad) {
			}
		};

		adChoicesContainer.setVisibility(View.INVISIBLE);
		mAdView.loadAd(mAdView.buildLoadAdConfig().withAdListener(adListener).build());
		adChoicesContainer.addView(mAdView);
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

	public void createInterstitial(int count) {
		String [] UnitIDArray = Config.firebaseData.getFacebookInterUnitID();
		int index = count % UnitIDArray.length;

//		Toast.makeText(mActivity, "index : " + index + " UnitIDArray: "
//				+ UnitIDArray[index] + " length : " + UnitIDArray.length, Toast.LENGTH_LONG).show();

		interstitialAd = new InterstitialAd(mActivity, UnitIDArray[index]);
		interstitialAdListener = new InterstitialAdListener() {
			@Override
			public void onInterstitialDisplayed(Ad ad) {
				adInterstitialAdLoaded = false;
				Log.e(TAG, "Ad Displayed");
			}

			@Override
			public void onInterstitialDismissed(Ad ad) {
				adInterstitialAdLoaded = false;
				requestNewInterstitial();
			}

			@Override
			public void onError(Ad ad, AdError adError) {
				adInterstitialAdLoaded = false;
				Log.e(TAG, "Error loading ad: " + adError.getErrorMessage());
			}

			@Override
			public void onAdLoaded(Ad ad) {
				adInterstitialAdLoaded = true;
				Log.e(TAG, "Ad Loaded");
			}

			@Override
			public void onAdClicked(Ad ad) {
				adInterstitialAdLoaded = false;
			}

			@Override
			public void onLoggingImpression(Ad ad) {
				adInterstitialAdLoaded = false;
			}
		};
		requestNewInterstitial();
	}

	private void requestNewInterstitial() {
		interstitialAd.loadAd(
				interstitialAd.buildLoadAdConfig()
						.withAdListener(interstitialAdListener)
						.build());

		show_interstitial_ad();
	}

	public void show_interstitial_ad() {
		if (interstitialAd == null) { return; }

		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if (interstitialAd.isAdLoaded()) {
					interstitialAd.show();
				}
			}
		});
	}

	public void onMainPause () {
	}

	public void onMainResume () {
	}

	public void onMainDestroy () {
		if (mAdView != null) {
			mAdView.destroy();
		}
	}
}
