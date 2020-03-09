package app.akilesh.ota.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import app.akilesh.ota.R
import app.akilesh.ota.databinding.ActivityMainBinding
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var updateIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val decorView = window.decorView
        decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                decorView.systemUiVisibility =
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        updateIntent = Intent(Intent.ACTION_MAIN)
        updateIntent.setClassName("com.google.android.gms","com.google.android.gms.update.SystemUpdateActivity")
        if (isCallable(updateIntent)) {
            binding.includeUpdateLink.check.setOnClickListener {
                startActivity(updateIntent)
            }
        }
        else binding.includeUpdateLink.check.visibility = View.GONE

        val isRoot = intent.getBooleanExtra("$packageName.isRoot", false)
        Log.d("isRoot", isRoot.toString())
        if (isRoot) {
            binding.includeUpdateLink.progressHorizontal.visibility = View.GONE
            binding.includeMessage.howToMessage.text = getString(R.string.reading_gms)
            readFromGMS()
        } else {
            binding.includeUpdateLink.check.visibility = View.GONE
            binding.includeMessage.howToMessage.text = getString(R.string.reading_logcat)
            readFromLogcat()
        }

    }

    private fun readFromLogcat() {
        val shell = Shell.newInstance()
        val timer = object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                shell.close()
                binding.includeUpdateLink.progressHorizontal.visibility = View.GONE
                binding.includeUpdateLink.link.text = getString(R.string.no_update_available)
                if (isCallable(updateIntent)) binding.includeUpdateLink.check.visibility = View.VISIBLE
            }
        }
        val callbackList =
            object : CallbackList<String>() {
                @MainThread
                override fun onAddElement(log: String) {
                    val result = Patterns.WEB_URL.toRegex().find(log)
                    if (result != null && result.value.contains("packages/ota-api")) {
                        binding.includeUpdateLink.progressHorizontal.visibility = View.GONE
                        postLink(result.value, "logcat")
                        shell.close()
                    }
                }
            }
        timer.start()
        shell.newJob().add("logcat").to(callbackList).submit()
    }

    private fun readFromGMS() {
        val filePath = "/data" + "/data/com.google.android.gms/shared_prefs/com.google.android.gms.update.storage.xml"

        if(Shell.su("[ -f $filePath ]").exec().isSuccess) {
            val file = File(filesDir, "update.xml")
            Shell.su(
                "cp -af $filePath ${file.absolutePath}",
                "chmod 664 ${file.absolutePath}"
            ).exec()
            val result = parseXML(file.reader())
            if (result != null) {
                binding.includeUpdateLink.check.visibility = View.GONE
                postLink(result, "gms")
            }
            else binding.includeUpdateLink.link.text = getString(R.string.no_update_available)
            file.delete()
        }
    }

    private fun postLink(url: String, from: String) {
        Log.d("$from-update-url", url)
        binding.includeUpdateLink.link.setTextIsSelectable(true)
        binding.includeUpdateLink.link.text = url
        val hint = getString(R.string.hint)
        if (!binding.includeMessage.howToMessage.text.contains(hint))
            binding.includeMessage.howToMessage.append(hint)
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
