package com.smartscan.pro.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object OcrEngine {

    suspend fun extractTextFromImages(
        context: Context,
        imagePaths: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val validFiles = imagePaths.map { File(it) }.filter { it.exists() }
        if (validFiles.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Koi valid page image nahi mili."))
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val combined = StringBuilder()
            validFiles.forEachIndexed { index, file ->
                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                val pageText = recognizeSingleImage(recognizer, inputImage)
                if (validFiles.size > 1) {
                    combined.append("--- Page ${index + 1} ---\n")
                }
                combined.append(pageText.trim())
                if (index < validFiles.lastIndex) {
                    combined.append("\n\n")
                }
            }

            val finalOutput = combined.toString().trim()
            if (finalOutput.isBlank()) {
                Result.failure(Exception("Is image mein koi readable text nahi mila. Clear photo ya Sharp Doc filter try karein."))
            } else {
                Result.success(finalOutput)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            recognizer.close()
        }
    }

    private suspend fun recognizeSingleImage(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        image: InputImage
    ): String = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText.text ?: "")
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume("")
                }
            }
    }

    fun saveTextToUri(context: Context, text: String, destinationUri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Extracted Text"))
    }
}

object AiAssistantEngine {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun askGemini(
        apiKey: String,
        documentTitle: String?,
        documentText: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Settings mein pehle apni Gemini API Key save karein.")
            )
        }

        val fullPrompt = buildString {
            append("You are Smart Scan AI Document Assistant. Give clear, accurate, concise answers.\n")
            if (documentText.isNotBlank()) {
                append("Attached Document Title: ${documentTitle ?: "Scanned Document"}\n")
                append("Document OCR Text:\n")
                append(documentText.take(16000))
                append("\n\n")
            }
            append("User Request: $userPrompt")
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", fullPrompt))
                    )
                )
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val apiError = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    val msg = apiError?.takeIf { it.isNotBlank() }
                        ?: "AI Server Error (HTTP ${response.code}). API Key ya internet check karein."
                    return@withContext Result.failure(Exception(msg))
                }

                val parsed = JSONObject(rawBody)
                val candidates = parsed.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val replyText = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

                if (replyText.isBlank()) {
                    Result.failure(Exception("AI se koi response text nahi aaya. Dobara try karein."))
                } else {
                    Result.success(replyText)
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Internet connection nahi hai ya server slow hai. Network check karein."))
        } catch (e: Exception) {
            Result.failure(Exception("AI error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}

object BiometricAuthHelper {

    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticate(activity)) {
            onError("Is phone mein Fingerprint ya Screen Lock set nahi hai. Phone Settings mein lock lagayein.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptBuilder
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
        }

        prompt.authenticate(promptBuilder.build())
    }
}
