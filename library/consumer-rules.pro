#-dontwarn com.cyber.ads.admob.AdmobUtils$BannerCallback
#-dontwarn com.cyber.ads.admob.AdmobUtils$InterCallback
#-dontwarn com.cyber.ads.admob.AdmobUtils$NativeCallback
#-dontwarn com.cyber.ads.admob.AdmobUtils$NativeCallbackSimple
#-dontwarn com.cyber.ads.admob.AdmobUtils$RewardCallback
#-dontwarn com.cyber.ads.admob.OnResumeUtils
#-keep class com.cyber.ads.admob.AdmobUtils { *; }
#-keep class com.cyber.ads.admob.RemoteUtils { *; }
#-keep class com.cyber.ads.utils.ExtensionKt { *; }
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