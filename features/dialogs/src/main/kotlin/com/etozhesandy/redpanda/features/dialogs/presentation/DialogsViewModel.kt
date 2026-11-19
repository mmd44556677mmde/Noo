package com.etozhesandy.redpanda.features.dialogs.presentation

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.DetectNewDialogsUseCase
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.ObserveImportProgressUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.common.mvi.getEnum
import com.etozhesandy.redpanda.core.common.mvi.putEnum
import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.model.nextAscending
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.ObserveAppSettingsUseCase
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.ObserveCategoriesUseCase
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.ObserveDialogsUseCase
import com.etozhesandy.redpanda.features.dialogs.domain.usecase.ObserveProfileUseCase
import com.etozhesandy.redpanda.features.dialogs.domain.model.sortedBy
import com.etozhesandy.redpanda.features.dialogs.model.DialogsArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DialogsViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val savedStateHandle: SavedStateHandle,
    args: DialogsArgs,
    private val observeDialogs: ObserveDialogsUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val detectNewDialogs: DetectNewDialogsUseCase,
    private val observeProfile: ObserveProfileUseCase,
    observeImportProgress: ObserveImportProgressUseCase,
    observeAppSettings: ObserveAppSettingsUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<DialogsState.State, DialogsState.Event, DialogsState.Effect>() {

    override fun createInitialState() = DialogsState.State()

    private val profileId = args.profileId
    private val query = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val category = MutableStateFlow<String?>(savedStateHandle.get<String>(KEY_CATEGORY))

    // Null means "nothing picked on this screen yet", so the default from the settings applies.
    // Picking a sort here is deliberately one-off: it never rewrites that default.
    private val sortOverride = MutableStateFlow(savedStateHandle.getEnum<DialogSort>(KEY_SORT))
    private val sortAscendingOverride = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_SORT_ASCENDING))

    private val effectiveSort = combine(sortOverride, sortAscendingOverride, observeAppSettings()) {
        override, ascendingOverride, settings ->
        (override ?: settings.defaultDialogSort) to (ascendingOverride ?: settings.defaultDialogSortAscending)
    }

    /** Seeds on first emission (no toast for the already-imported dialogs a user opens into). */
    private var knownDialogIds: Set<String>? = null

    init {
        // Read outside setState: inside its lambda these names resolve to the state's own
        // properties, not to the flows restored above.
        val restoredQuery = query.value
        val restoredCategory = category.value
        setState {
            copy(
                profileId = profileId,
                query = restoredQuery,
                selectedCategory = restoredCategory,
            )
        }

        effectiveSort
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)

        observeProfile(profileId)
            .onEach { profile -> setState { copy(profileStatus = profile?.status) } }
            .launchIn(viewModelScope)

        observeImportProgress(profileId)
            .onEach { progress -> setState { copy(importProgress = progress) } }
            .launchIn(viewModelScope)

        observeCategories(profileId)
            .onEach { categories -> setState { copy(categories = categories) } }
            .launchIn(viewModelScope)

        combine(query, category) { q, c -> q to c }
            .flatMapLatest { (q, c) -> observeDialogs(profileId, q, c) }
            .combine(effectiveSort) { dialogs, (s, asc) -> dialogs.sortedBy(s, asc) }
            .flowOn(defaultDispatcher)
            .onEach { dialogs -> setState { copy(dialogs = dialogs, isLoading = false) } }
            .launchIn(viewModelScope)

        // Independent of the search/category filter above, so switching filters mid-import never
        // misreads "already seen" dialogs as new ones.
        observeDialogs(profileId, null, null)
            .onEach { allDialogs -> onDialogsSnapshot(allDialogs) }
            .launchIn(viewModelScope)
    }

    private fun onDialogsSnapshot(dialogs: List<ChatDialog>) {
        if (currentState.profileStatus == ProfileStatus.IMPORTING) {
            detectNewDialogs(knownDialogIds, dialogs).forEach { dialog ->
                setEffect { DialogsState.Effect.DialogAdded(dialog.peerName) }
            }
        }
        knownDialogIds = dialogs.mapTo(HashSet()) { it.id }
    }

    override fun onEvent(event: DialogsState.Event) {
        when (event) {
            is DialogsState.Event.QueryChanged -> {
                savedStateHandle[KEY_QUERY] = event.value
                query.value = event.value
                setState { copy(query = event.value) }
            }
            is DialogsState.Event.CategorySelected -> {
                savedStateHandle[KEY_CATEGORY] = event.category
                category.value = event.category
                setState { copy(selectedCategory = event.category) }
            }
            is DialogsState.Event.DialogClicked ->
                nav.navigate(Routes.Chat(event.dialog.id, event.dialog.profileId))
            DialogsState.Event.SearchClicked -> nav.navigate(Routes.GlobalSearch(profileId))
            DialogsState.Event.FavoritesClicked -> nav.navigate(Routes.Favorites(profileId))
            DialogsState.Event.BackClicked -> nav.back()
            is DialogsState.Event.SortSelected -> {
                val ascending = nextAscending(
                    picked = event.sort,
                    current = currentState.sort,
                    currentAscending = currentState.sortAscending,
                    natural = event.sort.naturalAscending,
                )
                savedStateHandle.putEnum(KEY_SORT, event.sort)
                savedStateHandle[KEY_SORT_ASCENDING] = ascending
                sortOverride.value = event.sort
                sortAscendingOverride.value = ascending
                setState { copy(sort = event.sort, sortAscending = ascending) }
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_SORT = "sort"
        const val KEY_SORT_ASCENDING = "sort_ascending"
    }
}
