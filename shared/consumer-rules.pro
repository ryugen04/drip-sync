# Consumer rules for shared module
# These rules will be applied to any app that uses this library

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep enum classes
-keepclassmembers enum com.dripsync.shared.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
