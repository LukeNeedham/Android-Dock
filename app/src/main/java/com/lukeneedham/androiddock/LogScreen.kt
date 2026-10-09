package com.lukeneedham.androiddock

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val REFRESH_INTERVAL_MS = 200L
private const val AT_BOTTOM_SLOP_PX = 48

/** Shows what [DockLog] has recorded, newest at the bottom, so gestures can be debugged on-device. */
@Composable
internal fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vertical = rememberScrollState()
    var text by remember { mutableStateOf("") }
    // Bumped to ask for a reload; a drag writes many lines a second, so reloads are batched.
    var reloads by remember { mutableIntStateOf(0) }
    var followNewest by remember { mutableStateOf(true) }
    val empty = stringResource(R.string.log_empty)
    val copied = stringResource(R.string.log_copied)

    DisposableEffect(Unit) {
        val listener: () -> Unit = { reloads++ }
        DockLog.addListener(listener)
        onDispose { DockLog.removeListener(listener) }
    }
    LaunchedEffect(reloads) {
        if (reloads > 0) delay(REFRESH_INTERVAL_MS)
        // Keep following the newest line only if the user was already at the bottom.
        val atBottom = vertical.maxValue - vertical.value <= AT_BOTTOM_SLOP_PX
        val log = withContext(Dispatchers.IO) { DockLog.read(context) }
        text = log.ifEmpty { empty }
        if (followNewest || atBottom) {
            followNewest = false
            // Wait for the new text to be laid out before scrolling to its end.
            androidx.compose.runtime.withFrameNanos { }
            vertical.scrollTo(vertical.maxValue)
        }
    }

    SubPage(R.string.log_title, onBack) {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextButton(onClick = { followNewest = true; reloads++ }) { Text(stringResource(R.string.log_refresh)) }
            TextButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Android Dock log", DockLog.read(context)))
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.log_copy)) }
            TextButton(onClick = {
                DockLog.clear(context)
                followNewest = true
                reloads++
            }) { Text(stringResource(R.string.log_clear)) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(vertical)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SelectionContainer {
                Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
