package com.etozhesandy.redpanda.features.settings.presentation

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.net.CacheInventory
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UsageState(val entries: List<CacheInventory.Entry> = emptyList(), val used: Long = 0, val device: Long = 1,
    val limit: Int = 0, val rules: Map<String,Int> = emptyMap())

@HiltViewModel
class StorageUsageViewModel @Inject constructor(@ApplicationContext private val context: Context, private val nav: INavigationManager) : ViewModel() {
    private val mutable = MutableStateFlow(UsageState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    init { update {} }
    fun back() = nav.back()
    fun clear(selected: Set<String>) { update { CacheInventory.clear(context,selected) } }
    fun limit(gb: Int) { update { CacheInventory.prefs(context).edit().putInt("limit",gb).apply(); CacheInventory.trim(context) } }
    fun rule(key: String,days: Int) { update { CacheInventory.prefs(context).edit().putInt(key,days).apply(); CacheInventory.trim(context) } }
    private fun update(action: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                action()
                val prefs = CacheInventory.prefs(context)
                mutable.value = UsageState(CacheInventory.entries(context),context.dataDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } + java.io.File(context.applicationInfo.sourceDir).length(),StatFs(context.dataDir.path).totalBytes,prefs.getInt("limit",0),prefs.all.mapNotNull { (k,v) -> (v as? Int)?.let { k to it } }.toMap())
            }
        }
    }
}
