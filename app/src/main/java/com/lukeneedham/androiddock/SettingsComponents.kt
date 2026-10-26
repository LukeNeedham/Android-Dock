package com.lukeneedham.androiddock

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.lukeneedham.common.ui.CrashesPage
import com.lukeneedham.common.ui.DebugPage
import com.lukeneedham.common.ui.LogsPage
import com.lukeneedham.common.ui.ProvideDebugSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The four screen corners as a box with a labelled button in each of its corners. The selected
 * corner is highlighted and carries a tick.
 */
@Composable
internal fun CornerPicker(selected: DockPrefs.Corner, onSelect: (DockPrefs.Corner) -> Unit) {
    val colors = AppTheme.current.colors
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(6.dp),
    ) {
        // Each corner's whole quarter of the box is the click area; the button drawn in it stays small.
        listOf(
            listOf(DockPrefs.Corner.TOP_LEFT, DockPrefs.Corner.TOP_RIGHT),
            listOf(DockPrefs.Corner.BOTTOM_LEFT, DockPrefs.Corner.BOTTOM_RIGHT),
        ).forEach { line ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                line.forEach { corner ->
                    val isSelected = corner == selected
                    val shape = RoundedCornerShape(8.dp)
                    val selectedColors = colors.primaryButton
                    val content = if (isSelected) selectedColors.content else colors.text
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(corner) }),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(
                                    when (corner) {
                                        DockPrefs.Corner.BOTTOM_RIGHT -> Alignment.BottomEnd
                                        DockPrefs.Corner.BOTTOM_LEFT -> Alignment.BottomStart
                                        DockPrefs.Corner.TOP_LEFT -> Alignment.TopStart
                                        DockPrefs.Corner.TOP_RIGHT -> Alignment.TopEnd
                                    },
                                )
                                .clip(shape)
                                .background(if (isSelected) selectedColors.background else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(stringResource(corner.label), color = content)
                            if (isSelected) {
                                Icon(
                                    painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = content,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: Int, description: Int?) {
    Text(
        stringResource(title),
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp),
        color = AppTheme.current.colors.text,
    )
    if (description != null) {
        Text(
            stringResource(description),
            fontSize = 14.sp, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 4.dp),
            color = AppTheme.current.colors.text,
        )
    }
}

/** Drawn over a mid-grey so a translucent colour can be judged. */
@Composable
internal fun ColorSwatch(argb: Int, modifier: Modifier) {
    Box(modifier.background(Color.Gray)) {
        Box(Modifier.fillMaxSize().background(Color(argb)))
    }
}

/** The bottom sheet shared by every colour setting: opacity, red, green and blue sliders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerSheet(title: Int, argb: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
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
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(stringResource(title), fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = AppTheme.current.colors.text)
            ColorSwatch(argb, Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp))
            ColorSliders(argb, onChange)
        }
    }
}

/** Opacity, red, green and blue sliders for [argb]. */
@Composable
internal fun ColorSliders(argb: Int, onChange: (Int) -> Unit) {
    listOf(
        R.string.color_alpha to 24,
        R.string.color_red to 16,
        R.string.color_green to 8,
        R.string.color_blue to 0,
    ).forEach { (name, shift) ->
        val value = (argb ushr shift) and 0xFF
        Text(stringResource(name, value), modifier = Modifier.padding(top = 8.dp), color = AppTheme.current.colors.text)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange((argb and (0xFF shl shift).inv()) or (it.toInt() shl shift)) },
            valueRange = 0f..255f,
            colors = appSliderColors(),
        )
    }
}
