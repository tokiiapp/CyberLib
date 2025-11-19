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
-keep class com.cyber.ads.solar.SolarUtils { *; }
-keep class com.cyber.ads.application.AdsApplication { *; }
-keep class com.cyber.ads.remote.** { *; }
-keep class com.cyber.ads.admob.OnResumeUtils { *; }
-keep class com.cyber.ads.utils.ExtensionKt { *; }
-keep class com.cyber.ads.custom.LoadingSize { *; }
-keep class com.android.billingclient.** { *; }
-keep public class com.cyber.ads.iap.** { public *; }
-dontwarn com.android.billingclient.**
#solar
-keep class com.se.** {*; }
-keep class route.**{*;}
-keep interface com.se.** {*; }
-keep interface route.**{*;}
-dontwarn com.se.**
-dontwarn org.json.**
-keep class org.json.**{*;}
# Google lib
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }
# If the OAID plugin is used, please add the following obfuscation strategy:
-keep class com.huawei.hms.**{*;}
-keep class com.hihonor.**{*;}
# Giữ Google Ads SDK
-keep class com.google.android.gms.ads.** { *; }

# Giữ OnPaidEventListener và các callback
-keep class com.google.android.gms.ads.OnPaidEventListener { *; }

# Nếu dùng Adjust hoặc Firebase cũng phải thêm
-keep class com.adjust.** { *; }
-keep class com.google.firebase.** { *; }
