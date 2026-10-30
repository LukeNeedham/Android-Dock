package com.lukeneedham.androiddock

import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel

/** The triggers page: gets [TriggerViewModel] and only calls [TriggerPageContent]. */
@Composable
internal fun TriggerPage(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    val viewModel = koinViewModel<TriggerViewModel>()
    TriggerPageContent(
        state = viewModel.uiState,
        onBack = onBack,
        footer = footer,
        header = header,
        onSelect = viewModel::onSelect,
        onAdd = viewModel::onAdd,
        onRemove = viewModel::onRemove,
        onCornerChange = viewModel::onCornerChange,
        onSliderChange = viewModel::onSliderChange,
        onButtonColorChange = viewModel::onButtonColorChange,
    )
}
