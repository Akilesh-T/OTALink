package app.akilesh.ota.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import app.akilesh.ota.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shell.su().exec()

        if (!Shell.rootAccess()) {
            Log.e("su", "Unable to get root access")

            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.no_root_access_title))
                .setMessage(getString(R.string.no_root_access_message))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.exit)) { _, _ ->
                    finish()
                }
                .create()
                .show()
        }
        else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}