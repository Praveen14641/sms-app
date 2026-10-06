package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    // REPLACE WITH YOUR ACTUAL SUPABASE DETAILS
    private val supabaseUrl = "https://xvquygyjmbybwqyxwryl.supabase.co/rest/v1/sms_messages"
    private val supabaseApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2cXV5Z3lqbWJ5YndxeXh3cnlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyNzExNDYsImV4cCI6MjEwNjg0NzE0Nn0.9ysziZUI1wCBsudtw2MTouFTC8wDeZT0S8RKdbzCvbY"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: "Unknown"
                val body = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                Log.d("SmsReceiver", "Received SMS from $sender: $body")
                sendToSupabase(sender, body, timestamp)
            }
        }
    }

    private fun sendToSupabase(sender: String, message: String, timestamp: Long) {
        thread {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("sender", sender)
                    put("message", message)
                    put("timestamp", timestamp)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(supabaseUrl)
                    .post(requestBody)
                    .addHeader("apikey", supabaseApiKey)
                    .addHeader("Authorization", "Bearer $supabaseApiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("SmsReceiver", "Supabase Response Code: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error forwarding SMS to Supabase", e)
            }
        }
    }
}
