package com.galacticai.autonet.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.galacticai.autonet.R
import com.galacticai.autonet.common.isTethered
import com.galacticai.autonet.common.tether


class UsbReceiver : BroadcastReceiver() {
    companion object {
        const val USB_STATE = "android.hardware.usb.action.USB_STATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == USB_STATE) {
            val isConnected = intent.getBooleanExtra("connected", false)
            if (!isConnected) return

            val sharedPreference =
                context.getSharedPreferences("root", AppCompatActivity.MODE_PRIVATE)
            val rooted = sharedPreference.getBoolean("rooted", false)

            if (rooted) {
                if (isTethered()) return
                Toast.makeText(context, R.string.enabling_usb_tethering, Toast.LENGTH_SHORT).show()
                tether()
            }
        }
    }
}