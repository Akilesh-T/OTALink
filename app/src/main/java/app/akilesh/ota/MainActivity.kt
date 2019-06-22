package app.akilesh.ota

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
*/


class MainActivity : AppCompatActivity() {
    private var tag: String = "OTA"
    private lateinit var textView: TextView
    private lateinit var url: String
    private lateinit var cmd: String

    @SuppressLint("SdCardPath")
    private var dataFile = "/data/data/com.google.android.gms/shared_prefs/com.google.android.gms.update.storage.xml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val decorView = window.decorView
        decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        if(File(dataFile).exists()) {
            cmd = "cat $dataFile"
            val msg = "Reading from $dataFile"
            Log.d(tag, msg)


           /* val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

            val name = getString(R.string.app_name)
            val descriptionText = "System update available"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel("ota", name, importance)
            mChannel.description = descriptionText

            val builder = NotificationCompat.Builder(this, "ota")
                .setSmallIcon(R.drawable.ic_system_update)
                .setContentTitle("System update available")
                .setContentText("Tap to get the link!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
            notificationManager.notify(1, builder.build())

            // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            /*
        else {
            cmd = "logcat | grep packages/ota"
            val msg = "Reading from logcat"
            Log.d(tag, msg)
           // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        */
        */

            val process = Runtime.getRuntime().exec("su")
            val `in` = process.inputStream
            val out = process.outputStream
            out.write(cmd.toByteArray())
            out.flush()
            out.close()
            process.waitFor()

            if (process.exitValue() != 0) {
                Log.e(tag, "Failed to obtain root")
            } else {
                var ch: Int
                val sb = StringBuilder()
                while (`in`.read().let { ch = it; it != -1 })
                    sb.append(ch.toChar())
                url = sb.toString().trim()
            }

            val regex = "(https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*(zip)".toRegex()
            val matchResult = regex.find(url)

            textView = findViewById(R.id.url)
            textView.setTextIsSelectable(true)
            textView.text = String.format("%s", matchResult?.value)

        }
        else{
            textView = findViewById(R.id.url)
            textView.text = String.format("%s", "No updates available.")
        }
    }
}
