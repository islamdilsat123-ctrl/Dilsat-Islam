package com.example.api

import com.example.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Request Models representing standard Gemini structures to ensure full compatibility.
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiManager {
    suspend fun generateBengaliReply(userPrompt: String, history: List<Content> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "দুঃখিত, গুগল এআই স্টুডিওর Secrets প্যানেলে আপনার GEMINI_API_KEY টিম বা অ্যাকাউন্ট সেটআপ করা নেই। দয়া করে কীটি সরবরাহ করুন যেন গল্প বলি অ্যাসিস্ট্যান্ট কাজ করতে পারে!"
        }

        // Combine history if any
        val contentsList = mutableListOf<Content>()
        contentsList.addAll(history)
        contentsList.add(Content(parts = listOf(Part(text = userPrompt))))

        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = "You are 'Golpo Bot', an AI Chat partner in Golpo Kori, a secure, modern Bengali messaging app. " +
                           "Your tone must be warm, enthusiastic, highly polite, and conversational. Always reply in pure, natural Bengali " +
                           "or with sweet English loan words where natural. You love to gossip, keep messages concise like a real chatting " +
                           "partner (max 2-3 sentences), and use occasional emojis (🤖, 😊, ❤️, 🇧🇩). Help users with their questions or just " +
                           "friendly gossip (Golpo kora)!"
                )
            )
        )

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = systemInstruction
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "গল্প বটের মাথায় কোনো কথা আসছে না! আবার ট্রাই করুন।"
        } catch (e: Exception) {
            "দুঃখিত, এআই সংযোগে একটু বিভ্রাট হয়েছে। অনুগ্রহ করে আবার বলুন। (${e.localizedMessage})"
        }
    }
}
