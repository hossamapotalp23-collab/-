package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_INSTRUCTION = """
        You are "Noor Al-Quran AI" (نور القرآن AI), a premium, respectful, and highly knowledgeable Islamic AI Assistant.
        Your goal is to explain Quranic words, answer Islamic questions using trusted authentic sources (The Holy Quran, Sahih Al-Bukhari, Sahih Muslim),
        suggest personalized reading or memorization plans, and explain verses in gentle, simple, and clear language.
        Always maintain an elegant, pious, and peaceful tone. 
        Where appropriate, provide both Arabic text and its English translation.
        Do not express any speculative opinions or debate controversial topics; rely strictly on mainstream, trusted scholarship.
    """

    suspend fun askAssistant(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or placeholder.")
            return@withContext "Noor Al-Quran AI Assistant is currently in preview mode. Please configure your GEMINI_API_KEY in the Secrets panel to activate full capabilities."
        }

        try {
            // Build the JSON request body
            val requestJson = JSONObject().apply {
                // systemInstruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", SYSTEM_INSTRUCTION)
                    }))
                })
                // contents
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API Call Failed. Code: ${response.code}, Body: $errBody")
                    return@withContext "Peace be upon you. I encountered an issue connecting to the knowledge base (Error ${response.code}). Please verify your network and credentials."
                }

                val resBody = response.body?.string()
                if (resBody.isNullOrEmpty()) {
                    return@withContext "Forgive me, but I received an empty response. Let us try to ask again."
                }

                val jsonResponse = JSONObject(resBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "I am unable to formulate an answer right now.")
                        }
                    }
                }
                "I am deeply sorry, but I could not retrieve an answer at this moment."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during askAssistant", e)
            "A connection issue occurred: ${e.localizedMessage}. Please check your internet connection."
        }
    }
}
