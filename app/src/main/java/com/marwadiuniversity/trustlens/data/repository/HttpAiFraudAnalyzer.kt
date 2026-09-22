package com.marwadiuniversity.trustlens.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.marwadiuniversity.trustlens.BuildConfig
import com.marwadiuniversity.trustlens.domain.engine.AiFraudAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.SensitiveDataRedactor
import com.marwadiuniversity.trustlens.domain.model.AiAnalysisRequest
import com.marwadiuniversity.trustlens.domain.model.AiAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpAiFraudAnalyzer(
    private val backendUrl: String = BuildConfig.AI_BACKEND_URL
) : AiFraudAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val mediaType =
        "application/json; charset=utf-8".toMediaType()

    override suspend fun analyze(
        request: AiAnalysisRequest
    ): Result<AiAnalysisResult> {

        return withContext(Dispatchers.IO) {
            try {
                val redactedContent =
                    SensitiveDataRedactor.redact(request.content)

                val sanitizedRequest =
                    request.copy(content = redactedContent)

                val jsonBody =
                    gson.toJson(sanitizedRequest)

                val body =
                    jsonBody.toRequestBody(mediaType)

                val currentUser =
                    FirebaseAuth.getInstance().currentUser
                        ?: return@withContext Result.failure(
                            Exception("User is not authenticated")
                        )

                val idToken = try {
                    currentUser
                        .getIdToken(false)
                        .await()
                        .token
                } catch (e: Exception) {
                    null
                } ?: return@withContext Result.failure(
                    Exception(
                        "Unable to obtain Firebase authentication token"
                    )
                )

                val httpRequest =
                    Request.Builder()
                        .url(backendUrl)
                        .addHeader(
                            "Authorization",
                            "Bearer $idToken"
                        )
                        .post(body)
                        .build()

                client.newCall(httpRequest).execute().use { response ->

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(
                                "Backend AI request failed with code ${response.code}"
                            )
                        )
                    }

                    val responseString =
                        response.body?.string()
                            ?: return@withContext Result.failure(
                                Exception("Empty response body")
                            )

                    val aiResult =
                        gson.fromJson(
                            responseString,
                            AiAnalysisResult::class.java
                        )

                    val clampedResult =
                        aiResult.copy(
                            riskScore =
                                aiResult.riskScore.coerceIn(0, 100)
                        )

                    Result.success(clampedResult)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
