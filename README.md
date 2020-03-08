# OTA Update Link
Capture the URL of an Android One OTA update

### Root method
  The updates to Android One devices are handled by Google Play services(GMS). GMS stores the update url in the tag `control.installation.current_update_url` of the file `/data/data/com.google.android.gms/shared_prefs/com.google.android.gms.update.storage.xml`.
The url is read from that file and displayed.
  
### Non-root method
  If the device is not rooted, a permission([android.permission.READ_LOGS](https://developer.android.com/reference/android/Manifest.permission#READ_LOGS)) has to be granted. Then the app can read the system logs and extract the update url.
Connect your device to a computer with Android [platform tools](https://developer.android.com/studio/releases/platform-tools) installed and run the following command (needs to be done only once):
`adb shell "pm grant app.akilesh.ota android.permission.READ_LOGS && am force-stop app.akilesh.ota"`

### [Screenshots](https://forum.xda-developers.com/devdb/project/?id=33300#screenshots) 
