package com.galacticai.autonet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.galacticai.autonet.common.isIgnoringBatteryOptimization
import com.galacticai.autonet.common.isOwnServiceRunning
import com.galacticai.autonet.common.shellRunAsRoot
import com.galacticai.autonet.receivers.BOOT_COMPLETED
import com.galacticai.autonet.receivers.BootReceiver
import com.galacticai.autonet.receivers.LOCKED_BOOT_COMPLETED
import com.galacticai.autonet.receivers.QUICKBOOT_POWERON
import com.galacticai.autonet.services.UsbService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView


class MainActivity : AppCompatActivity() {
    private lateinit var bootReceiver: BootReceiver

    private lateinit var usbDetectionIntent: Intent

    private lateinit var serviceSwitch: MaterialSwitch
    private lateinit var serviceStatusLabel: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bootReceiver.isInitialized) unregisterReceiver(bootReceiver)
    }

    private fun init() {
        initVersionLabel()
        initRoot(this)
        initUsbDetection()
        initBootReceiver()
        initBatteryOptimization()
        initNotificationPermission()
    }


    private fun initVersionLabel() {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        @SuppressLint("SetTextI18n") // it's ok for version string
        findViewById<TextView>(R.id.versionLabel).text = "v${pInfo.versionName}"
    }

    private fun initUsbDetection() {
        fun serviceSwitchCheckedChange(isChecked: Boolean, isInit: Boolean = false) {

            val preference = getSharedPreferences("root", MODE_PRIVATE)
            val rooted = preference.getBoolean("rooted", false)
            if (!rooted && isChecked) {
                initRoot(this)
                serviceSwitch.isChecked = false
                return
            }

            val textID: Int
            val colorID: Int

            if (isChecked) {
                textID = R.string.service_status_label_enabled
                colorID = R.color.primary
                if (!isInit) startService(usbDetectionIntent)
            } else {
                textID = R.string.service_status_label_disabled
                colorID = R.color.disabled
                if (!isInit) stopService(usbDetectionIntent)
            }

            serviceStatusLabel.apply {
                text = getString(textID)
                setTextColor(getColor(colorID))
            }
        }

        usbDetectionIntent = Intent(this, UsbService::class.java)

        serviceSwitch = findViewById(R.id.serviceSwitch)
        serviceStatusLabel = findViewById(R.id.serviceStatusLabel)

        val isServiceRunning = isOwnServiceRunning(this, UsbService::class.java)
        serviceSwitchCheckedChange(isServiceRunning, true)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            serviceSwitchCheckedChange(isChecked)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBootReceiver() {
        bootReceiver = BootReceiver()
        val bootIntentFilter: IntentFilter = IntentFilter().apply {
            addAction(BOOT_COMPLETED)
            addAction(QUICKBOOT_POWERON)
            addAction(LOCKED_BOOT_COMPLETED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(bootReceiver, bootIntentFilter, RECEIVER_EXPORTED)
        else registerReceiver(bootReceiver, bootIntentFilter)
    }


    @SuppressLint("BatteryLife")
    private fun initBatteryOptimization() {
        if (isIgnoringBatteryOptimization(this)) return

        MaterialAlertDialogBuilder(this).apply {
            setIcon(
                AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.baseline_battery_saver_64
                )
            )
            setTitle(getString(R.string.battery_optimization_detected))
            setMessage(getString(R.string.battery_optimization_warning))
            setNegativeButton("${getString(R.string.skip)} (${getString(R.string.not_recommended)})") { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(R.string.ok) { dialog, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${this@MainActivity.packageName}")
                    )
                )
                if (isIgnoringBatteryOptimization(this@MainActivity))
                    dialog.dismiss()
            }
            show()
        }
    }

    private fun initNotificationPermission() {
        val permission =
            ActivityCompat.checkSelfPermission(applicationContext, UsbService.POST_NOTIFICATIONS)

        if (permission == PackageManager.PERMISSION_GRANTED) return

        MaterialAlertDialogBuilder(this).apply {
            setIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_notifications_24))
            setTitle(getString(R.string.enable_notification))
            setMessage(getString(R.string.notification_dialog_text))
            setNegativeButton(R.string.skip) { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton(R.string.enable) { dialog, _ ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(UsbService.POST_NOTIFICATIONS),
                    R.id.persistent_notification
                )
                dialog.dismiss()
            }
            show()
        }
    }

    companion object {
        fun initRoot(context: Context) {
            MaterialAlertDialogBuilder(context).apply {
                setIcon(AppCompatResources.getDrawable(context, R.drawable.round_tag_24))
                setTitle(context.getString(R.string.root_required))
                setMessage(context.getString(R.string.root_dialog_text))
                setPositiveButton(R.string.enable) { dialog, _ ->
                    val preference = context.getSharedPreferences("root", MODE_PRIVATE)
                    val editor = preference.edit()
                    try {
                        val ls = shellRunAsRoot("ls /system")
                        editor.putBoolean("rooted", ls.isNotEmpty())
                    } catch (_: Exception) {
                        editor.putBoolean("rooted", false)
                    }
                    editor.apply()
                    dialog.dismiss()
                }
                show()
            }
        }
    }
}