package com.psp.demo;

import android.graphics.Bitmap;

import com.psp.demo.utils.GetOutData;

public class FirebaseData {

	//Unity Ads implementation
	private String UnityGameId;
	private String UnityAdInterUnitId;
	private String UnityAdBannerUnitId;
	private boolean UnityEnable;

	//Admob Ads implementation
	private String AdmobBannerUnitId;
	private String [] AdmobInterUnitID;
	private boolean AdmobEnable;

	//facebook Ads Implementation
	private String FacebookBannerUnitId;
	private String [] FacebookInterUnitId;
	private boolean FacebookEnable;

	//Ironsource Ads Implementation
	private String ISApkKey;
	private boolean MediationIS;

	//Time Interval
	private int AdsInterval;

	// Custom Ads implementation
	private Bitmap CustomPlayButton;
	private Bitmap CustomBackground;
	private Bitmap CustomMoreButton;
	private Bitmap CustomPolicyButton;
	private String IsoGame;



	public FirebaseData(String unityGameId, String UnityAdBannerId, String UnityAdInterId, boolean unityEnable, String admobBanner,
						String [] admobInter, /*String admobInter2, String admobInter3,*/
						boolean admobEnable, String fanBanner, String [] fanInter, /*String fanInter2,
                        String fanInter3,*/ boolean fanEnable, String isApkKey,boolean mediationIS,
						int adsInterval, Bitmap customPlayBotton, Bitmap customBackground, Bitmap moreButton, Bitmap policyButton, String isoGame) {
		// Unity Ads implementation
		this.UnityGameId = unityGameId;
		this.UnityEnable = unityEnable;
		this.UnityAdInterUnitId = UnityAdInterId;
		this.UnityAdBannerUnitId = UnityAdBannerId;

		//Admob Ads implementation
		this.AdmobBannerUnitId = admobBanner;
		this.AdmobInterUnitID = admobInter;
//        this.AdmobInterUnitID2 = admobInter2;
//        this.AdmobInterUnitID3 = admobInter3;
		this.AdmobEnable = admobEnable;

		// facebook Ads implementation
		this.FacebookBannerUnitId = fanBanner;
		this.FacebookInterUnitId = fanInter;
//        this.FacebookInterUnitId2 = fanInter2;
//        this.FacebookInterUnitId3 = fanInter3;
		this.FacebookEnable = fanEnable;

		// Ironsource Ads implementation
		this.ISApkKey = isApkKey;
		this.MediationIS = mediationIS;

		// time interval
		this.AdsInterval = adsInterval;

		// Custom Ads implementation
		this.CustomPlayButton = customPlayBotton;
		this.CustomBackground = customBackground;
		this.CustomMoreButton = moreButton;
		this.CustomPolicyButton = policyButton;
		this.IsoGame = isoGame;
	}

	public String getUnityGameId(){
		return UnityGameId;
	}

	public void setUnityGameId(String unityGameId){
		UnityGameId = unityGameId;
	}

	public boolean getUnityEnable(){
		return UnityEnable;
	}

	public String getUnityAdInterUnitId(){
		return UnityAdInterUnitId;
	}

	public String getUnityAdBannerUnitId(){
		return UnityAdBannerUnitId;
	}

	public void setUnityEnable(boolean unityEnable){
		UnityEnable = unityEnable;
	}

	public String getAdmobBannerUnitId(){
		return AdmobBannerUnitId;
	}

	public void setAdmobBannerUnitId(String admobBannerUnitId){
		AdmobBannerUnitId = admobBannerUnitId;
	}

	public String [] getAdmobInterUnitID(){
		return AdmobInterUnitID;
	}


//    public String getSecondAdmobInterUnitID(){
//        return AdmobInterUnitID2;
//    }
//
//    public void setSecondAdmobInterUnitID(String admobInterUnitID){
//        AdmobInterUnitID2 = admobInterUnitID;
//    }
//
//    public String getThirdAdmobInterUnitID(){
//        return AdmobInterUnitID3;
//    }
//
//    public void setThirdAdmobInterUnitID(String admobInterUnitID){
//        AdmobInterUnitID3 = admobInterUnitID;
//    }

	public boolean getAdmobEnable(){
		return AdmobEnable;
	}

	public void setAdmobEnable(boolean admobEnable){
		AdmobEnable = admobEnable;
	}

	public String getFacebookBannerUnitId(){
		return FacebookBannerUnitId;
	}

	public void setFacebookBannerUnitId(String fanBanner){
		FacebookBannerUnitId = fanBanner;
	}

	public String [] getFacebookInterUnitID(){
		return FacebookInterUnitId;
	}

//    public String getSecondFacebookInterUnitID(){
//        return FacebookInterUnitId2;
//    }
//
//    public void setSecondFacebookInterUnitID(String fanInterUnitID){
//        FacebookInterUnitId2 = fanInterUnitID;
//    }
//
//    public String getThirdFacebookInterUnitID(){
//        return FacebookInterUnitId3;
//    }
//
//    public void setThirdFacebookInterUnitID(String fanInterUnitID){
//        FacebookInterUnitId3 = fanInterUnitID;
//    }

	public boolean getFacebookEnable(){
		return FacebookEnable;
	}

	public void setFacebookEnable(boolean fanEnable){
		FacebookEnable = fanEnable;
	}

	public String getISApkKey() {
		return ISApkKey;
	}

	public boolean getMediationIS(){
		return MediationIS;
	}

	public void setMediationIS(boolean mediationIS){
		MediationIS = mediationIS;
	}

	public int getAdsInterval(){
		return AdsInterval;
	}

	public void setAdsInterval(int adsInterval){
		AdsInterval = adsInterval;
	}

	public Bitmap getCustomPlayButton(){
		return CustomPlayButton;
	}

	public void setCustomPlayButton(Bitmap customPlayButton){
		CustomPlayButton = customPlayButton;
	}

	public Bitmap getCustomBackground(){
		return CustomBackground;
	}

	public void setCustomBackground(Bitmap customBackground){
		CustomBackground = customBackground;
	}

	public Bitmap getCustomMoreButton(){
		return CustomMoreButton;
	}

	public Bitmap getCustomPolicyButton(){
		return CustomPolicyButton;
	}

	public String getIsoGame(){
		return IsoGame;
	}
}
