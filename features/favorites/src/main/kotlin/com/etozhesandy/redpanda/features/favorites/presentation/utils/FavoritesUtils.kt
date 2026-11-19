package com.etozhesandy.redpanda.features.favorites.presentation.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timestampFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

fun formatFavoriteTimestamp(epochMillis: Long): String = timestampFormat.format(Date(epochMillis))
