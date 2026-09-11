package com.automatelinux.surveySend.data

import kotlinx.serialization.Serializable

/**
 * The wire shapes, matching what the daemon emits with `--json 1`.
 *
 * Every field that can be absent has a default. The daemon is the authority for
 * what a survey is and it will grow fields; a phone that refuses to parse a
 * reply carrying one more key than it knows about is a phone that breaks on the
 * next daemon deploy.
 */
@Serializable
data class Survey(
    val id: String = "",
    val createdAt: String = "",
    val app: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val job: String = "",
    val surveyUrl: String = "",
    val status: String = "",
    val sent: Boolean = false,
    val answeredAt: String = "",
    val productRating: Int = 0,
    val serviceRating: Int = 0,
    val comment: String = "",
)

@Serializable
data class SendResponse(
    val ok: Boolean = false,
    val survey: Survey? = null,
    val sent: Boolean = false,
    val sendError: String = "",
    val error: String = "",
)

@Serializable
data class SurveysResponse(
    val ok: Boolean = false,
    val surveys: List<Survey> = emptyList(),
    val error: String = "",
)
