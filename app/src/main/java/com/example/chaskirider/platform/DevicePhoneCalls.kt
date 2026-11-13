package com.example.chaskirider.platform

import com.example.chaskirider.feature.orders.domain.PhoneCalls
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.example.chaskirider.feature.orders.domain.PhoneCallState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class DevicePhoneCalls(private val context: Context) : com.example.chaskirider.feature.orders.domain.PhoneCalls {
    override fun states() = callbackFlow {
        check(context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)) {
            "Este dispositivo no admite llamadas celulares"
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) throw SecurityException("Falta el permiso de estado telefónico")
        val manager = context.getSystemService(TelephonyManager::class.java)
        fun emit(state: Int) {
            val value = when (state) {
                TelephonyManager.CALL_STATE_IDLE -> PhoneCallState.IDLE
                TelephonyManager.CALL_STATE_RINGING -> PhoneCallState.RINGING
                TelephonyManager.CALL_STATE_OFFHOOK -> PhoneCallState.OFFHOOK
                else -> return
            }
            trySend(value)
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = emit(state)
            }
            manager.registerTelephonyCallback(context.mainExecutor, callback)
            awaitClose { manager.unregisterTelephonyCallback(callback) }
        } else {
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) = emit(state)
            }
            manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            awaitClose { manager.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }
    }

    override fun start(number: String) {
        require(Regex("\\+?[0-9]{9,15}").matches(number)) { "Número de teléfono inválido" }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) throw SecurityException("Falta el permiso de llamadas")
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
