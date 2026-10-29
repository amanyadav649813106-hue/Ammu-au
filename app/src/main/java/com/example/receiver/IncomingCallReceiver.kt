package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import com.example.service.DeviceActionManager

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                val callerName = resolveContactName(context, incomingNumber)

                val announcement = if (!callerName.isNullOrBlank()) {
                    "कमांडर! $callerName का कॉल आ रहा है! क्या कॉल उठाना है या कट करना है?"
                } else if (!incomingNumber.isNullOrBlank()) {
                    "कमांडर! नंबर $incomingNumber से कॉल आ रहा है! क्या कॉल उठाना है या कट करना है?"
                } else {
                    "कमांडर! एक कॉल आ रहा है! क्या कॉल उठाना है या कट करना है?"
                }

                DeviceActionManager.setRingingCallState(true, callerName ?: incomingNumber ?: "अज्ञात")
                DeviceActionManager.speakDirectly(context, announcement)
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE || state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                DeviceActionManager.setRingingCallState(false, "")
            }
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
