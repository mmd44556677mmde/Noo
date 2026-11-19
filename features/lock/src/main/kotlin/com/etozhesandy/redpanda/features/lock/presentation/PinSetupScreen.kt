package com.etozhesandy.redpanda.features.lock.presentation

import com.etozhesandy.redpanda.features.lock.presentation.view.PinSetupMessage
import com.etozhesandy.redpanda.features.lock.presentation.utils.promptRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.model.PinSetupStep
import com.etozhesandy.redpanda.features.lock.presentation.utils.formatCountdown
import com.etozhesandy.redpanda.features.lock.presentation.view.PinDots
import com.etozhesandy.redpanda.features.lock.presentation.view.PinKeypad

/** Sets, changes or removes the login PIN, one four-digit step at a time. */
@Composable
fun PinSetupScreen(
    state: PinSetupState.State,
    onEvent: (PinSetupState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.pin_setup_title),
        modifier = modifier,
        onBack = { onEvent(PinSetupState.Event.BackClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(state.step.promptRes()),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PinDots(
                filled = state.pin.length,
                length = AppLockConfig.PIN_LENGTH,
                isError = state.errorRes != null,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PinSetupMessage(state = state)
            Spacer(modifier = Modifier.height(16.dp))
            PinKeypad(
                onDigit = { onEvent(PinSetupState.Event.PinDigitEntered(it)) },
                onBackspace = { onEvent(PinSetupState.Event.PinBackspacePressed) },
                enabled = state.isKeypadEnabled,
            )
        }
    }
}
