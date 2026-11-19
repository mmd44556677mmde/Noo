package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.core.model.ChatDialog
import javax.inject.Inject

/**
 * Finds the dialogs in a fresh snapshot that the reader has not been shown yet, so an import that
 * is still running can announce each one as it appears.
 *
 * [known] is null before the first snapshot: the dialogs already imported when the screen opens
 * are not news, so nothing is reported until there is something to compare against.
 */
class DetectNewDialogsUseCase @Inject constructor() {
    operator fun invoke(known: Set<String>?, current: List<ChatDialog>): List<ChatDialog> =
        if (known == null) emptyList() else current.filter { it.id !in known }
}
