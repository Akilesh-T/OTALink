package app.akilesh.ota.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import androidx.appcompat.app.AppCompatActivity
import app.akilesh.ota.R
import app.akilesh.ota.databinding.ActivityMainBinding
import com.topjohnwu.superuser.Shell
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var filePath = "/data" + "/data/com.google.android.gms/shared_prefs/com.google.android.gms.update.storage.xml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val linkTextView = binding.include.link

        val decorView = window.decorView
        decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                decorView.systemUiVisibility =
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }


        if(Shell.su("[ -f $filePath ]").exec().isSuccess) {

            val file = File(filesDir, "update.xml")
            Shell.su(
                "cp -af $filePath ${file.absolutePath}",
                "chmod 664 ${file.absolutePath}"
            ).exec()
            val result = parseXML(file.reader())
            if (result != null) {
                Log.d("update-url", result)
                binding.include.check.visibility = View.GONE
                linkTextView.setTextIsSelectable(true)
                linkTextView.text = String.format("%s", result)
                //hintText.text = String.format("%s", resources.getString(R.string.hint))
            } else {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.setClassName("com.google.android.gms","com.google.android.gms.update.SystemUpdateActivity")
                if (isCallable(intent)) {
                    binding.include.check.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            startActivity(intent)
                        }
                    }
                }
                else binding.include.check.visibility = View.GONE

                linkTextView.text = String.format("%s", getString(R.string.no_update_available))
            }
        }
    }

    private fun isCallable(intent: Intent): Boolean {
        val activities = packageManager.queryIntentActivities(intent,
            PackageManager.MATCH_DEFAULT_ONLY)
        return activities.isNotEmpty()
    }

    private fun parseXML(reader: InputStreamReader): String? {
        val xmlPullParserFactory = XmlPullParserFactory.newInstance()
        xmlPullParserFactory.isNamespaceAware = true
        val xmlPullParser = xmlPullParserFactory.newPullParser()
        xmlPullParser.setInput(reader)
        var eventType = xmlPullParser.eventType
        var tag: String? = null
        var attr: String? = null
        var updateURL: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("map" != xmlPullParser.name) {
                    tag = xmlPullParser.name
                    attr = xmlPullParser.getAttributeValue(null, "name")
                }
            }
            else if (eventType == XmlPullParser.END_TAG) tag = null
            else if (eventType == XmlPullParser.TEXT) {
                if (tag != null && attr == "control.installation.current_update_url") {
                    updateURL = xmlPullParser.text
                }
            }
            eventType = xmlPullParser.next()
        }
        return updateURL
    }

}
