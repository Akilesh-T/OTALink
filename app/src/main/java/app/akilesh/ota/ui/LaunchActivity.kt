package app.akilesh.ota.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.akilesh.ota.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shell.su().exec()

        val mainIntent = Intent(this, MainActivity::class.java)

        if (!Shell.rootAccess()) {
            Log.w("su", "Unable to get root access")

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
                mainIntent.putExtra("$packageName.isRoot", Shell.rootAccess())
                startActivity(mainIntent)
                finish()
            }
            else {
                val adbCommand = "adb shell \"pm grant $packageName ${Manifest.permission.READ_LOGS} && am force-stop $packageName\""
                val alertDialog = MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.no_root_access_title))
                    .setMessage(String.format(getString(R.string.grant_permission_message), getString(R.string.app_name), adbCommand))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.copy_command), null)
                    .setNegativeButton(getString(R.string.exit)) { _, _ ->
                        finish()
                    }
                    .create()
                alertDialog.setOnShowListener {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val clipboard =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText(
                                "adb command", adbCommand
                            )
                            clipboard.setPrimaryClip(clipData)
                            Toast.makeText(this, getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
                        }
                }
                alertDialog.show()
            }
        }
        else {
            mainIntent.putExtra("$packageName.isRoot", Shell.rootAccess())
            startActivity(mainIntent)
            finish()
        }
    }
}
