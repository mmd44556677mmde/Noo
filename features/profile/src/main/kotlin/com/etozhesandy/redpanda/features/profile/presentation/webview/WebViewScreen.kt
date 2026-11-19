package com.etozhesandy.redpanda.features.profile.presentation.webview

import com.etozhesandy.redpanda.features.profile.utils.copyToClipboard
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.Icons
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.features.profile.R

/** Dump links can be copied, but are never loaded by the reader. */
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val copyLabel = stringResource(R.string.profile_link_copy_label)
    BaseScreen(
        title = url,
        modifier = modifier,
        onBack = onBack,
        actions = {
            // The link shown in the title, not the address the page ends up on: VK redirects to
            // its mobile site, and that is not the link the reader wants to share.
            IconButton(
                onClick = { context.copyToClipboard(copyLabel, url, R.string.profile_link_copied) },
                enabled = UrlGuard.isVkUrl(url),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.profile_link_copy))
            }
        },
    ) {
        EmptyState(text = stringResource(R.string.profile_link_blocked))
    }
}
