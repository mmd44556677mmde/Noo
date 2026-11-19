package com.etozhesandy.redpanda.features.settings.presentation.utils

import android.content.Context

fun Context.appVersionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull()
        .orEmpty()
        .ifEmpty { "—" }
