package id.example.smssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val deviceToken = prefs.getString("device_token", "").orEmpty()
        if (deviceToken.isBlank()) { pending.finish(); return }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        val receivedAt = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        thread {
            try {
                val payload = org.json.JSONObject().apply {
                    put("sender", sender)
                    put("body", body)
                    put("receivedAt", java.time.Instant.ofEpochMilli(receivedAt).toString())
                    put("deviceId", android.os.Build.MANUFACTURER + "-" + android.os.Build.MODEL)
                }
                val connection = (URL(MainActivity.API_BASE_URL.trimEnd('/') + "/api/messages")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $deviceToken")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                connection.inputStream.close()
                connection.disconnect()
            } catch (_: Exception) {
                // Production version should persist failed uploads and retry with WorkManager.
            } finally {
                pending.finish()
            }
        }
    }
}
