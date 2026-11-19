package com.etozhesandy.redpanda.features.profile.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Puts [text] on the clipboard. From Android 13 the system shows its own confirmation of a copy,
 * and a second message of ours would only repeat it; older versions say nothing, so [confirmationRes]
 * is shown there.
 */
fun Context.copyToClipboard(label: String, text: String, @StringRes confirmationRes: Int) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, confirmationRes, Toast.LENGTH_SHORT).show()
    }
}
