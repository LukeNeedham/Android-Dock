package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

/** The background page: gets [FanBackgroundViewModel] and only calls [FanBackgroundPageContent]. */
@Composable
internal fun FanBackgroundPage(onBack: (() -> Unit)?) {
    val viewModel = koinViewModel<FanBackgroundViewModel>()
    FanBackgroundPageContent(
        state = viewModel.uiState,
        onBack = onBack,
        onStopsChange = viewModel::onStopsChange,
    )
}
