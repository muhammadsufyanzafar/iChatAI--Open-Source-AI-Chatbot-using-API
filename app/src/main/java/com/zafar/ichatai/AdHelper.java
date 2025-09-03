package com.zafar.ichatai;

import android.app.Activity;

import androidx.annotation.MainThread;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.concurrent.atomic.AtomicReference;

public class AdHelper {

    // Test IDs — replace with your live IDs when ready
    private static final String INTERSTITIAL_ID = "ca-app-pub-4884978624677986/9143435806";
    private static final String REWARDED_ID = "ca-app-pub-4884978624677986/2380240731";

    private static final AtomicReference<InterstitialAd> interstitialRef = new AtomicReference<>(null);
    private static final AtomicReference<RewardedAd> rewardedRef = new AtomicReference<>(null);

    public interface InterstitialCallback { void onClosed(boolean shown); }
    public interface RewardCallback { void onRewarded(boolean rewarded); }

    private static void loadInterstitial(Activity a) {
        InterstitialAd.load(a, INTERSTITIAL_ID, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) { interstitialRef.set(ad); }
            @Override public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError e) { interstitialRef.set(null); }
        });
    }

    @MainThread
    public static void showInterstitial(Activity a, InterstitialCallback cb) {
        InterstitialAd ad = interstitialRef.get();
        if (ad == null) {
            loadInterstitial(a);
            cb.onClosed(false);
            return;
        }
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                interstitialRef.set(null);
                loadInterstitial(a);
                cb.onClosed(true);
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                interstitialRef.set(null);
                cb.onClosed(false);
            }
        });
        ad.show(a);
    }

    private static void loadRewarded(Activity a) {
        RewardedAd.load(a, REWARDED_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) { rewardedRef.set(ad); }
            @Override public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError e) { rewardedRef.set(null); }
        });
    }

    @MainThread
    public static void showRewarded(Activity a, RewardCallback cb) {
        RewardedAd ad = rewardedRef.get();
        if (ad == null) {
            loadRewarded(a);
            cb.onRewarded(false);
            return;
        }
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                rewardedRef.set(null);
                loadRewarded(a);
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedRef.set(null);
                cb.onRewarded(false);
            }
        });
        ad.show(a, (RewardItem rewardItem) -> cb.onRewarded(true));
    }

    public static void warmUp(Activity a) {
        loadInterstitial(a);
        loadRewarded(a);
    }
}