-keep class com.cyber.ads.admob.AdmobUtils$BannerCallback { *; }
-keep class com.cyber.ads.admob.AdmobUtils$InterCallback { *; }
-keep class com.cyber.ads.admob.AdmobUtils$NativeCallback { *; }
-keep class com.cyber.ads.admob.AdmobUtils$NativeCallbackSimple { *; }
-keep class com.cyber.ads.admob.AdmobUtils$RewardCallback { *; }

-keep class com.cyber.ads.admob.AdmobUtils {
    public *;
 }
-keep class com.cyber.ads.admob.RemoteUtils { *; }
-keep class com.cyber.ads.adjust.AdjustUtils { *; }
-keep class com.cyber.ads.application.AdsApplication { *; }
-keep class com.cyber.ads.remote.** { *; }
-keep class com.cyber.ads.admob.OnResumeUtils { *; }
-keep class com.cyber.ads.utils.ExtensionKt { *; }
-keep class com.cyber.ads.custom.LoadingSize { *; }
-keep class com.android.billingclient.** { *; }
-keep public class com.cyber.ads.iap.** { public *; }
-dontwarn com.android.billingclient.**
