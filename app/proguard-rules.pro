# Sensus - Proguard / R8 Optimization & Obfuscation Rules

# ====================================================================
# SQLCipher for Android (JNI native bindings: mNativeHandle, etc.)
# ====================================================================
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**

# ====================================================================
# Kotlinx Serialization & Navigation 3
# ====================================================================
-keepattributes *Annotation*,InnerClasses,Signature
-dontnote kotlinx.serialization.SerializationKt

# Keep companion objects and generated serializers for @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class **
-keep,allowobfuscation class <1>$$serializer { *; }

-keep,allowobfuscation,includedescriptorclasses @kotlinx.serialization.Serializable class ** { *; }

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Navigation 3 keys & polymorphic routes
-keep class com.example.sensus.NavigationKeys { *; }
-keep class com.example.sensus.NavigationKeys$** { *; }
-keep class com.example.sensus.SensusRoute { *; }
-keep class com.example.sensus.SensusRoute$** { *; }

# ====================================================================
# Data Models
# ====================================================================
-keep class com.example.sensus.data.** { *; }

# ====================================================================
# Jetpack Compose & Navigation
# ====================================================================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

-dontwarn androidx.compose.**

# ====================================================================
# ViewModels & Architecture Components
# ====================================================================
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ====================================================================
# Coroutines & Maps
# ====================================================================
-dontwarn kotlinx.coroutines.**
-dontwarn org.osmdroid.**

# ====================================================================
# Tink (androidx.security:security-crypto optional cloud KMS downloader)
# ====================================================================
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
