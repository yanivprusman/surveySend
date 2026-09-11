package com.automatelinux.surveySend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.surveySend.data.Survey
import com.automatelinux.surveySend.data.SurveyApi
import com.automatelinux.surveySend.platform.rememberContactPicker
import com.automatelinux.surveySend.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * סקר — one screen, because there is only one thing to do.
 *
 * You have just finished a job and you are still standing there. Three fields,
 * one button, and underneath it what people have said about the last jobs.
 * Nothing is behind a menu: a second screen is a second thing to find while you
 * are holding a ladder.
 *
 * The form does NOT re-implement the rules. Whether a number is a real Israeli
 * mobile is the daemon's business, and its refusal is shown verbatim. The one
 * thing checked here is that a field isn't empty, which is not a rule so much
 * as a reason not to spend a round trip.
 */

private fun stars(n: Int): String =
    if (n <= 0) "—" else "★".repeat(n) + "☆".repeat(5 - n)

/** A one-line banner: the result of the last thing he did. */
private data class Banner(val text: String, val good: Boolean)

@Composable
fun App(baseUrl: String, token: String) {
    val api = remember(baseUrl, token) { SurveyApi(baseUrl, token) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var app by remember { mutableStateOf("") }

    // The picker fills the NUMBER and nothing else — the same rule the גבייה app
    // learned the hard way. An address-book label is a private note about how to
    // FIND someone ("יוסי אינסטלטור", "דוד מחסן עמק חפר"); it is not the name to
    // greet a customer by, and no rule can tell which words are the person and
    // which are the filing note. The name is typed by the one person who knows
    // it. An entry already typed is left alone.
    val pickContact = rememberContactPicker { c ->
        phone = c.phone.filter { it.isDigit() || it == '+' }
    }

    var busy by remember { mutableStateOf(false) }
    var loadingList by remember { mutableStateOf(true) }
    var banner by remember { mutableStateOf<Banner?>(null) }
    // A connection problem is a STATE, not an event: it must disappear by itself
    // the moment the connection comes back, so it does not live in `banner`
    // alongside one-off results.
    var listError by remember { mutableStateOf("") }
    var surveys by remember { mutableStateOf<List<Survey>>(emptyList()) }

    suspend fun refresh() {
        loadingList = true
        val r = api.list()
        if (r.ok) {
            surveys = r.surveys
            listError = ""
        } else if (r.error.isNotEmpty()) {
            listError = r.error
        }
        loadingList = false
    }

    LaunchedEffect(Unit) { refresh() }

    val canSend = name.isNotBlank() && phone.isNotBlank() && job.isNotBlank() && !busy

    fun send() {
        if (!canSend) return
        busy = true
        banner = null
        scope.launch {
            val r = api.send(name = name.trim(), phone = phone.trim(), job = job.trim(), app = app.trim())
            busy = false
            when {
                r.ok && r.sent -> {
                    banner = Banner("נשלח ל${name.trim()}", good = true)
                    // Only the customer is cleared. The job description and the
                    // app tag survive on purpose: the next survey is usually the
                    // next flat on the same round, and retyping "ניקוי חלונות"
                    // twenty times is how a tool stops being used.
                    name = ""; phone = ""
                    refresh()
                }
                r.ok -> {
                    // The survey exists; the WhatsApp leg failed. Not an error
                    // screen — the link is real and he can pass it on himself.
                    banner = Banner(
                        "השאלון נוצר אבל ההודעה לא יצאה. הקישור: ${r.survey?.surveyUrl ?: ""}",
                        good = false,
                    )
                    refresh()
                }
                else -> banner = Banner(r.error.ifEmpty { "לא הצלחנו לשלוח" }, good = false)
            }
        }
    }

    // The whole interface is Hebrew, so the direction is a property of the app
    // rather than of the phone's locale — a device set to English would
    // otherwise render this right-aligned text in a left-to-right frame.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AppTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Scaffold(containerColor = Color.Transparent) { inner ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        contentPadding = PaddingValues(
                            top = inner.calculateTopPadding() + 20.dp,
                            bottom = inner.calculateBottomPadding() + 32.dp,
                            start = 20.dp,
                            end = 20.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "סקר",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        answeredSummary(surveys),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { scope.launch { refresh() } }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "רענן")
                                }
                            }
                        }

                        banner?.let { b ->
                            item { BannerCard(b) { banner = null } }
                        }

                        item {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("שם הלקוח") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        OutlinedTextField(
                                            value = phone,
                                            onValueChange = { phone = it },
                                            label = { Text("טלפון") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Phone,
                                                imeAction = ImeAction.Next,
                                            ),
                                        )
                                        IconButton(onClick = { pickContact() }) {
                                            Icon(Icons.Filled.Contacts, contentDescription = "בחר מאנשי הקשר")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = job,
                                        onValueChange = { job = it },
                                        label = { Text("מה עשית אצלו") },
                                        placeholder = { Text("מחסן פאנל 2x2 — התקנה") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    )
                                    OutlinedTextField(
                                        value = app,
                                        onValueChange = { app = it },
                                        label = { Text("עסק (לא חובה)") },
                                        placeholder = { Text("panelShed") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    )
                                    Button(
                                        onClick = { send() },
                                        enabled = canSend,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        if (busy) {
                                            CircularProgressIndicator(
                                                Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        } else {
                                            Text("שלח שאלון", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (listError.isNotEmpty()) {
                            item {
                                Text(
                                    listError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        if (loadingList && surveys.isEmpty()) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        items(surveys, key = { it.id }) { s -> SurveyRow(s) }

                        if (!loadingList && surveys.isEmpty() && listError.isEmpty()) {
                            item {
                                Text(
                                    "עוד לא שלחת שאלונים.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The one number worth a headline: how many of the people you asked replied. */
private fun answeredSummary(surveys: List<Survey>): String {
    if (surveys.isEmpty()) return "אף אחד עוד לא נשאל"
    val answered = surveys.count { it.status == "answered" }
    return "$answered מתוך ${surveys.size} ענו"
}

@Composable
private fun BannerCard(b: Banner, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (b.good) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                b.text,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (b.good) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onDismiss) { Text("סגור") }
        }
    }
}

/**
 * One survey in the list.
 *
 * An answered one shows what he said; an unanswered one shows only that it was
 * asked. Deliberately not a link to the survey page — that page is the
 * CUSTOMER'S, and opening it from here would let the owner answer his own
 * survey with the one answer it accepts.
 */
@Composable
private fun SurveyRow(s: Survey) {
    val answered = s.status == "answered"
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    s.customerName.ifEmpty { s.customerPhone },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    if (answered) "ענה" else if (s.sent) "נשלח" else "לא נשלח",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                s.job,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (answered) {
                Spacer(Modifier.height(2.dp))
                Text("מוצר ${stars(s.productRating)}    שירות ${stars(s.serviceRating)}")
                if (s.comment.isNotEmpty()) {
                    Text(
                        "“${s.comment}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
