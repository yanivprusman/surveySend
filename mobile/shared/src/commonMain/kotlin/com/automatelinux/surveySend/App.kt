package com.automatelinux.surveySend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.automatelinux.surveySend.ui.theme.SurveyColors
import com.automatelinux.surveySend.ui.theme.starOffColor
import kotlinx.coroutines.launch

/**
 * סקר — one screen, because there is only one thing to do.
 *
 * You have just finished a job and you are still standing there. Three fields,
 * one button, and underneath it what people said about the last jobs. Nothing
 * is behind a menu: a second screen is a second thing to find while you are
 * holding a ladder.
 *
 * The form does NOT re-implement the rules. Whether a number is a real Israeli
 * mobile is the daemon's business, and its refusal is shown verbatim. The one
 * thing checked here is that a field isn't empty, which is not a rule so much
 * as a reason not to spend a round trip.
 */

/** A one-line banner: the result of the last thing he did. */
private data class Banner(val text: String, val good: Boolean)

/** "2026-09-11 16:14:02" → "11.09 · 16:14". The daemon's format is fixed, so
 *  this reads it by position rather than pulling in a date library for one
 *  label. Anything unexpected is shown as-is instead of being mangled. */
private fun shortWhen(raw: String): String {
    if (raw.length < 16) return raw
    val d = raw.substring(8, 10)
    val m = raw.substring(5, 7)
    val t = raw.substring(11, 16)
    return "$d.$m · $t"
}

@Composable
fun App(baseUrl: String, token: String) {
    val api = remember(baseUrl, token) { SurveyApi(baseUrl, token) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var app by remember { mutableStateOf("") }

    // The picker fills the NUMBER and nothing else — the rule the גבייה app
    // learned the hard way. An address-book label is a private note about how to
    // FIND someone ("יוסי אינסטלטור", "דוד מחסן עמק חפר"); it is not the name to
    // greet a customer by, and nothing in the string says which words are the
    // person and which are the filing note. An entry already typed is left alone.
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
            val r = api.send(name.trim(), phone.trim(), job.trim(), app.trim())
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

    // The whole interface is Hebrew, so direction is a property of the APP
    // rather than of the phone's locale — a device set to English would
    // otherwise render this right-aligned text in a left-to-right frame.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AppTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Scaffold(containerColor = Color.Transparent) { inner ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        contentPadding = PaddingValues(
                            top = inner.calculateTopPadding() + 18.dp,
                            bottom = inner.calculateBottomPadding() + 36.dp,
                            start = 18.dp,
                            end = 18.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item { Header(surveys, loadingList) { scope.launch { refresh() } } }

                        banner?.let { b -> item { BannerCard(b) { banner = null } } }

                        item {
                            ComposeCard(
                                name = name, onName = { name = it },
                                phone = phone, onPhone = { phone = it },
                                job = job, onJob = { job = it },
                                app = app, onApp = { app = it },
                                onPickContact = pickContact,
                                canSend = canSend, busy = busy, onSend = { send() },
                            )
                        }

                        if (listError.isNotEmpty()) {
                            item { InlineError(listError) }
                        }

                        if (surveys.isNotEmpty()) {
                            item {
                                Text(
                                    "נשלחו לאחרונה",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                                )
                            }
                        }

                        items(surveys, key = { it.id }) { s -> SurveyRow(s) }

                        if (loadingList && surveys.isEmpty()) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.5.dp)
                                }
                            }
                        }

                        if (!loadingList && surveys.isEmpty() && listError.isEmpty()) {
                            item { EmptyState() }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The header, and the one number worth a headline.
 *
 * The response RATE, not the count: "12 נשלחו" tells him he has been busy,
 * which he knows. "7 מתוך 12 ענו" tells him whether asking works at all, which
 * is the only thing on this screen he could decide something from.
 */
@Composable
private fun Header(surveys: List<Survey>, loading: Boolean, onRefresh: () -> Unit) {
    val answered = surveys.count { it.status == "answered" }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "סקר",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            if (surveys.isEmpty()) {
                Text(
                    if (loading) "טוען…" else "עוד לא שאלת אף אחד",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$answered",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        " מתוך ${surveys.size} ענו",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "רענן",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The form. A bordered card rather than a floating one: this is a sheet of
 *  paper to fill in, and a drop shadow would make it hover for no reason. */
@Composable
private fun ComposeCard(
    name: String, onName: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    job: String, onJob: (String) -> Unit,
    app: String, onApp: (String) -> Unit,
    onPickContact: (() -> Unit)?,
    canSend: Boolean, busy: Boolean, onSend: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = name, onValueChange = onName,
                label = { Text("שם הלקוח") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = phone, onValueChange = onPhone,
                    label = { Text("טלפון") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next,
                    ),
                )
                // Null on a platform with no picker — the button is hidden
                // rather than shown doing nothing.
                onPickContact?.let { pick ->
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { pick() }) {
                            Icon(
                                Icons.Filled.Contacts,
                                contentDescription = "בחר מאנשי הקשר",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = job, onValueChange = onJob,
                label = { Text("מה עשית אצלו") },
                placeholder = { Text("מחסן פאנל 2x2 — התקנה") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = app, onValueChange = onApp,
                label = { Text("עסק (לא חובה)") },
                placeholder = { Text("panelShed") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("שלח שאלון", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BannerCard(b: Banner, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (b.good) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                b.text,
                Modifier.weight(1f).padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (b.good) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onDismiss) {
                Text(
                    "סגור",
                    color = if (b.good) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun InlineError(text: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** Five glyphs, not a string of "★★★☆☆": the lit ones have to be gold and the
 *  unlit ones grey, and one Text cannot be two colours. */
@Composable
private fun Stars(n: Int, size: Int = 15) {
    val off = starOffColor()
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { i ->
            Text(
                "★",
                fontSize = size.sp,
                color = if (i < n) SurveyColors.Gold else off,
            )
        }
    }
}

/** A small status word on a tinted pill — scannable down a list in a way that
 *  a line of grey text is not. */
@Composable
private fun StatusChip(answered: Boolean, sent: Boolean) {
    val (label, bg, fg) = when {
        answered -> Triple("ענה", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        sent -> Triple("נשלח", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple("לא נשלח", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Box(
        Modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.customerName.ifEmpty { s.customerPhone },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        s.job,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(answered, s.sent)
            }

            if (answered) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("מוצר", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Stars(s.productRating)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("שירות", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Stars(s.serviceRating)
                    }
                }
                if (s.comment.isNotEmpty()) {
                    Text(
                        "“${s.comment}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    shortWhen(s.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (s.app.isNotEmpty()) {
                    Text(
                        s.app,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The empty state carries the app's own mark rather than a shrug — the first
 *  thing a new user sees should look finished. */
@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("★", fontSize = 30.sp, color = SurveyColors.Gold)
        }
        Text(
            "עוד לא שלחת שאלונים",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "סיימת עבודה? מלא למעלה ושלח — לוקח דקה, והתשובות יופיעו כאן.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
