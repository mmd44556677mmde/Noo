package com.etozhesandy.redpanda.features.lock.presentation.utils

import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.model.PinSetupStep

fun PinSetupStep.promptRes(): Int = when (this) {
    PinSetupStep.CURRENT -> R.string.pin_setup_enter_current
    PinSetupStep.NEW -> R.string.pin_setup_enter_new
    PinSetupStep.CONFIRM -> R.string.pin_setup_confirm_new
}
