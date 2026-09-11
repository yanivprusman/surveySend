package com.automatelinux.surveySend.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One HTTP round trip. Android supplies it with the JDK client; iOS will
 *  supply NSURLSession. Kept this small so neither platform grows a second
 *  place where a request can be built differently. */
expect suspend fun httpRequest(
    method: String,
    url: String,
    token: String,
    jsonBody: String?,
): HttpResult

data class HttpResult(val code: Int, val body: String, val transportError: String? = null)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The app's whole conversation with the backend: send a survey, list surveys.
 *
 * Failures are values, never exceptions thrown at the UI: this screen is used
 * standing in someone's garden five minutes after finishing the job, so "the
 * VPN is not up" has to render as a sentence rather than as a crash or a
 * spinner that never ends.
 */
class SurveyApi(baseUrl: String, private val token: String) {
    private val base = baseUrl.trimEnd('/')

    val configured: Boolean get() = token.isNotEmpty()

    suspend fun list(): SurveysResponse {
        if (!configured) return SurveysResponse(error = NO_TOKEN)
        val r = httpRequest("GET", "$base/api/surveys?status=all", token, null)
        return runCatching { json.decodeFromString(SurveysResponse.serializer(), r.body) }
            .getOrElse { SurveysResponse(error = describe(r)) }
    }

    suspend fun send(
        name: String,
        phone: String,
        job: String,
        app: String,
    ): SendResponse {
        if (!configured) return SendResponse(error = NO_TOKEN)
        val body = json.encodeToString(
            SendRequest.serializer(),
            SendRequest(to = phone, name = name, job = job, app = app),
        )
        val r = httpRequest("POST", "$base/api/survey", token, body)
        return runCatching { json.decodeFromString(SendResponse.serializer(), r.body) }
            .getOrElse { SendResponse(error = describe(r)) }
    }

    /** What to show when the reply was not the JSON we expected — the status
     *  code alone is useless to someone holding a phone. */
    private fun describe(r: HttpResult): String = when (r.code) {
        401 -> "המכשיר לא מורשה מול השרת (טוקן שגוי)"
        0 -> "אין חיבור לשרת — בדוק שה‑VPN פעיל"
        else -> "השרת החזיר תשובה לא צפויה (${r.code})"
    }

    private companion object {
        const val NO_TOKEN = "הגרסה הזו נבנתה בלי טוקן — בנה מחדש עם mobile/.env"
    }
}

@Serializable
private data class SendRequest(
    val to: String,
    val name: String,
    val job: String,
    val app: String,
)
