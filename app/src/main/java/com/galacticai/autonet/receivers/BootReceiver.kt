package com.galacticai.autonet.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.galacticai.autonet.services.UsbService


const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
const val LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (intent.action == BOOT_COMPLETED
            || intent.action == QUICKBOOT_POWERON
            || intent.action == LOCKED_BOOT_COMPLETED
        ) {
            val serviceIntent = Intent(context, UsbService::class.java)
            context?.startService(serviceIntent)
        }
    }

}