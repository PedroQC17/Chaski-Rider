# Gson y modelos persistidos localmente.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.example.chaskirider.feature.rider.domain.RiderOnboardingDraft { *; }
-keep class com.example.chaskirider.feature.rider.domain.PendingDocumentCapture { *; }
-keep class com.example.chaskirider.feature.support.domain.SupportDraft { *; }
-keep class com.example.chaskirider.feature.orders.data.local.DemoOrder { *; }
-keep class com.example.chaskirider.feature.earnings.data.local.** { *; }

-dontwarn org.jetbrains.annotations.**
