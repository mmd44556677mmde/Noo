package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.etozhesandy.redpanda.core.common.net.DumpMedia

/** Single-use approval, scoped to the URL shown by this control. */
@Composable
fun withRemoteConsent(url: String, action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var ask by remember(url) { mutableStateOf(false) }
    if (ask) AlertDialog(onDismissRequest = { ask = false }, text = { Text("Fetch this item from its VK media host?") },
        confirmButton = { TextButton({ ask = false; DumpMedia.approve(url); action() }) { Text("Fetch") } },
        dismissButton = { TextButton({ ask = false }) { Text("Cancel") } })
    return { if (url.startsWith("https://", true) && DumpMedia.policy(context) == DumpMedia.Policy.ASK) ask = true else action() }
}
