# Security & Anti-Tampering Hardening Rules for Ammu AI
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep data models used with JSON and Moshi
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.data.local.entities.** { *; }
-keep class com.example.data.remote.** { *; }

# Protect SecurityShieldManager
-keep class com.example.service.SecurityShieldManager { *; }
-keep class com.example.service.DeviceSecurityReport { *; }

# Keep Receivers and Services intact
-keep class com.example.service.AmmuBackgroundVoiceService { *; }
-keep class com.example.service.AmmuAccessibilityService { *; }
-keep class com.example.receiver.IncomingCallReceiver { *; }
-keep class com.example.receiver.AmmuDeviceAdminReceiver { *; }
-keep class com.example.receiver.BootReceiver { *; }

# Prevent tampering with Room database
-keep class * extends androidx.room.RoomDatabase
