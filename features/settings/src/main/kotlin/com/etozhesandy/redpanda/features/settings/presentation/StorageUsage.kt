package com.etozhesandy.redpanda.features.settings.presentation

import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.common.net.CacheInventory as Inventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun StorageUsage(back: () -> Unit) {
    BackHandler(onBack = back)
    val c = LocalContext.current
    val scope = rememberCoroutineScope(); val prefs = remember { Inventory.prefs(c) }
    var entries by remember { mutableStateOf(emptyList<Inventory.Entry>()) }
    var used by remember { mutableLongStateOf(0) }; var device by remember { mutableLongStateOf(1) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        withContext(Dispatchers.IO) {
            entries = Inventory.entries(c); device = StatFs(c.dataDir.path).totalBytes
            used = c.dataDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } + java.io.File(c.applicationInfo.sourceDir).length()
        }
    }
    val total = entries.sumOf { it.size }; val colors = listOf(Color.Blue,Color.Magenta,Color.Cyan,Color.Green,Color.Gray)
    fun size(n: Long) = android.text.format.Formatter.formatShortFileSize(c,n)
    var checked by remember { mutableStateOf(Inventory.categories.toSet()) }
    var tab by remember { mutableStateOf("Chats") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = back) { Text("‹ Settings") }; Text("Storage usage", style = MaterialTheme.typography.headlineMedium)
        Canvas(Modifier.size(120.dp).padding(10.dp)) {
            var angle = -90f
            Inventory.categories.forEachIndexed { i, cat ->
                val sweep = 360f * entries.filter { it.category == cat }.sumOf { it.size } / total.coerceAtLeast(1)
                drawArc(colors[i], angle, sweep, false, style = Stroke(12.dp.toPx())); angle += sweep
            }
        }
        Text("App uses %.2f%% of device storage".format(100.0 * used / device.coerceAtLeast(1)))
        Inventory.categories.forEach { cat ->
            val bytes = entries.filter { it.category == cat }.sumOf { it.size }
            Row { Checkbox(cat in checked, { checked = if(it) checked + cat else checked - cat }); Text("$cat   ${size(bytes)}   %.1f%%".format(100.0*bytes/total.coerceAtLeast(1))) }
        }
        Button(onClick = { scope.launch { withContext(Dispatchers.IO) { Inventory.clear(c,checked) }; refresh++ } }, enabled = checked.isNotEmpty()) { Text("Clear cache") }
        Text("Cleared media downloads again on next open. Imported originals are kept.")
        Text("Auto-remove cached media")
        var cat by remember { mutableIntStateOf(0) }; var kind by remember { mutableIntStateOf(0) }
        val kinds = listOf("PERSON","GROUP","PROFILE"); val key = "${Inventory.categories[cat]}:${kinds[kind]}"
        var days by remember(key) { mutableIntStateOf(prefs.getInt(key,0)) }
        Row { TextButton({cat=(cat+1)%4}) { Text(Inventory.categories[cat]) }; TextButton({kind=(kind+1)%3}) { Text(kinds[kind]) }; TextButton({days=when(days){0->2;2->30;else->0};prefs.edit().putInt(key,days).apply()}) { Text(when(days){0->"Never";2->"2 days";else->"1 month"}) } }
        var limit by remember { mutableFloatStateOf(Inventory.limits.indexOf(prefs.getInt("limit",0)).toFloat()) }
        Text("Maximum cache size: " + Inventory.limits[limit.roundToInt()].let { if(it==0) "No limit" else "$it GB" })
        Slider(limit,{limit=it},valueRange=0f..3f,steps=2,onValueChangeFinished={prefs.edit().putInt("limit",Inventory.limits[limit.roundToInt()]).apply();scope.launch { withContext(Dispatchers.IO){Inventory.trim(c)};refresh++ }})
        Row { listOf("Chats","Media","Files","Music").forEach { TextButton({tab=it}) { Text(it) } } }
        entries.filter { it.size>0 && when(tab){"Media"->it.category in listOf("Photos","Videos");"Music"->it.category=="Audio";"Files"->it.category=="Documents";else->true} }.groupBy { listOfNotNull(it.profile,it.chat).joinToString(" · ").ifBlank { "Unassigned" } }.mapValues { it.value.sumOf { e->e.size } }.toList().sortedByDescending { it.second }.forEach { Text("${it.first}    ${size(it.second)}") }
    }
}
