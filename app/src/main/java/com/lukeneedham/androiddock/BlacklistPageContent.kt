package com.lukeneedham.androiddock

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The apps that never show in the sheet, on a page of their own. */
@Composable
internal fun BlacklistPageContent(
    state: BlacklistUiState,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit,
    onPickerOpen: () -> Unit,
) {
    SubPage(R.string.blacklist_title, onBack, footer, header) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                stringResource(R.string.blacklist_description),
                fontSize = 14.sp, lineHeight = 20.sp,
                color = AppTheme.current.colors.secondaryText,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BlacklistEditor(state, onRemove, onAdd, onPickerOpen)
            NavBarSpacer()
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (app.icon != null) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Text(app.label, modifier = Modifier.weight(1f).padding(start = 12.dp), color = AppTheme.current.colors.text)
        trailing()
    }
}

/** The blacklist: the apps that never show in the sheet, with a way to add and remove them. */
@Composable
private fun BlacklistEditor(state: BlacklistUiState, onRemove: (String) -> Unit, onAdd: (String) -> Unit, onPickerOpen: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var picking by remember { mutableStateOf(false) }

    if (state.hidden.isEmpty()) {
        Text(
            stringResource(R.string.blacklist_empty),
            fontSize = 14.sp, lineHeight = 20.sp,
            color = AppTheme.current.colors.secondaryText,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    state.hidden.forEach { app ->
        AppRow(app) {
            AppButton(text = stringResource(R.string.blacklist_remove), style = AppButtonStyle.Text, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRemove(app.packageName)
            })
        }
    }
    AppButton(
        text = stringResource(R.string.blacklist_add),
        style = AppButtonStyle.Secondary,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPickerOpen()
            picking = true
        },
        modifier = Modifier.padding(top = 16.dp),
    )

    if (picking) {
        AppPickerSheet(
            apps = state.pickableApps,
            exclude = state.hidden.map { it.packageName }.toSet(),
            onPick = {
                onAdd(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** A bottom sheet listing the installed apps, for choosing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(apps: List<AppEntry>?, exclude: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.current.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bottomSheet,
        contentColor = colors.text,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
    ) {
        Text(
            stringResource(R.string.blacklist_pick_title),
            fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = AppTheme.current.colors.text,
        )
        var query by remember { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.blacklist_search), color = colors.fieldLabel) },
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.fieldFocusedBorder,
                unfocusedBorderColor = colors.fieldBorder,
                focusedLabelColor = colors.fieldLabel,
                unfocusedLabelColor = colors.fieldLabel,
                cursorColor = colors.fieldFocusedBorder,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val list = apps
        if (list == null) {
            CircularProgressIndicator(color = AppTheme.current.colors.accent, modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                items(
                    list.filter {
                        it.packageName !in exclude && it.label.contains(query.trim(), ignoreCase = true)
                    },
                    key = { it.packageName }) { app ->
                    AppRow(app, Modifier.clickable { onPick(app.packageName) })
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        BlacklistPageContent(
            state = BlacklistUiState(hidden = listOf(AppEntry("com.example.news", "News", null), AppEntry("com.example.maps", "Maps", null))),
            onBack = {},
            onRemove = {},
            onAdd = {},
            onPickerOpen = {},
        )
    }
}
