# Notif Tiles: the widget calls framework methods by name (RemoteViews) and the system starts our services
# and activities from the manifest, so nothing special has to be kept. Keep line numbers for readable crashes.
-keepattributes SourceFile,LineNumberTable
-dontwarn com.google.android.gms.**
