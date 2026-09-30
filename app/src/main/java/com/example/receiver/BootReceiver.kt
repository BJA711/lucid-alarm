package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("BootReceiver", "Received action: $action, rescheduling active alarms")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmApp.instance.repository.rescheduleAllEnabled()
                        Log.d("BootReceiver", "Successfully restored scheduled alarms")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Failed to reschedule alarms on boot", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
