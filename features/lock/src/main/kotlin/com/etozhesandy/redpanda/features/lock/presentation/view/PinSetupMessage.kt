package com.etozhesandy.redpanda.features.lock.presentation.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.PinSetupState
import com.etozhesandy.redpanda.features.lock.presentation.utils.formatCountdown

/** The countdown takes priority: while it runs, no other error is worth reading. */
@Composable
fun PinSetupMessage(state: PinSetupState.State) {
    val text = when {
        state.lockoutRemainingMs > 0L ->
            stringResource(R.string.lock_locked_out, formatCountdown(state.lockoutRemainingMs))
        state.errorRes == null -> null
        state.errorArg == null -> stringResource(state.errorRes)
        else -> stringResource(state.errorRes, state.errorArg)
    }
    Text(
        text = text.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
