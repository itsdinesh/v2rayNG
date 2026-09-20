package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.enums.AppRoutingAction
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.ItemDivider
import com.v2ray.ang.ui.compose.NavigationBarsBottomPadding
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.AppIconFetcher
import com.v2ray.ang.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SetAllProxy(R.string.menu_item_set_all_proxy),
    SetAllDirect(R.string.menu_item_set_all_direct),
    SetAllBlock(R.string.menu_item_set_all_block),
    ResetAll(R.string.menu_item_reset_all),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val directApps by viewModel.directApps.collectAsStateWithLifecycle()
        val blockApps by viewModel.blockApps.collectAsStateWithLifecycle()
        val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
        val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            directApps = directApps,
            blockApps = blockApps,
            filterCategory = filterCategory,
            totalCount = viewModel.totalCount,
            proxyCount = viewModel.proxyCount,
            directCount = viewModel.directCount,
            blockCount = viewModel.blockCount,
            perAppProxyEnabled = perAppProxyEnabled,
            onBackClick = { finish() },
            onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
            onFilterCategoryChanged = { viewModel.setFilterCategory(it) },
            onSetAppRouting = { pkg, action -> viewModel.setAppRouting(pkg, action) },
            onSearch = { viewModel.filterApps(it) },
            onSetAllDisplayed = { viewModel.setAllDisplayed(it) },
            onResetAll = { viewModel.resetAll() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    directApps: Set<String>,
    blockApps: Set<String>,
    filterCategory: RoutingFilterCategory,
    totalCount: Int,
    proxyCount: Int,
    directCount: Int,
    blockCount: Int,
    perAppProxyEnabled: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onFilterCategoryChanged: (RoutingFilterCategory) -> Unit,
    onSetAppRouting: (String, AppRoutingAction) -> Unit,
    onSearch: (String) -> Unit,
    onSetAllDisplayed: (AppRoutingAction) -> Unit,
    onResetAll: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    val onInfoClick = { showInfoDialog = true }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.per_app_proxy_settings),
                onBackClick = onBackClick,
                isLoading = isLoading,
                isSearchActive = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    onSearch(query)
                },
                onSearchClose = {
                    searchQuery = ""
                    onSearch("")
                    showSearch = false
                },
                searchPlaceholder = stringResource(R.string.menu_item_search),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.acc_search)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(R.string.acc_more)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            AppDropdownMenuItems(PerAppMenuAction.entries, { it.labelRes }) { action ->
                                showMenu = false
                                when (action) {
                                    PerAppMenuAction.SetAllProxy -> onSetAllDisplayed(AppRoutingAction.PROXY)
                                    PerAppMenuAction.SetAllDirect -> onSetAllDisplayed(AppRoutingAction.DIRECT)
                                    PerAppMenuAction.SetAllBlock -> onSetAllDisplayed(AppRoutingAction.BLOCK)
                                    PerAppMenuAction.ResetAll -> onResetAll()
                                    PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                    PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                    PerAppMenuAction.ExportSelection -> onExportProxyApp()
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.per_app_proxy_settings_enable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = perAppProxyEnabled,
                                modifier = Modifier.scale(0.8f),
                                onCheckedChange = onPerAppProxyChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                        IconButton(onClick = onInfoClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_about_24dp),
                                contentDescription = stringResource(R.string.acc_per_app_proxy_information),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    RoutingCategoryFilters(
                        selectedCategory = filterCategory,
                        onCategorySelected = onFilterCategoryChanged,
                        totalCount = totalCount,
                        proxyCount = proxyCount,
                        directCount = directCount,
                        blockCount = blockCount,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }
            AppDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = NavigationBarsBottomPadding()
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    val currentAction = when {
                        blockApps.contains(app.packageName) -> AppRoutingAction.BLOCK
                        directApps.contains(app.packageName) -> AppRoutingAction.DIRECT
                        else -> AppRoutingAction.PROXY
                    }
                    PerAppRoutingListItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        currentAction = currentAction,
                        onActionChanged = { action -> onSetAppRouting(app.packageName, action) }
                    )
                    ItemDivider()
                }
            }
        }
    }

    if (showInfoDialog) {
        ConfirmDialog(
            message = stringResource(R.string.summary_pref_per_app_proxy),
            dismissText = null,
            onConfirm = {},
            onDismiss = { showInfoDialog = false },
        )
    }
}

@Composable
private fun RoutingCategoryFilters(
    selectedCategory: RoutingFilterCategory,
    onCategorySelected: (RoutingFilterCategory) -> Unit,
    totalCount: Int,
    proxyCount: Int,
    directCount: Int,
    blockCount: Int,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(RoutingFilterCategory.ALL, stringResource(R.string.per_app_proxy_filter_all), totalCount),
        Triple(RoutingFilterCategory.PROXY, stringResource(R.string.per_app_proxy_mode_proxy), proxyCount),
        Triple(RoutingFilterCategory.DIRECT, stringResource(R.string.per_app_proxy_mode_direct), directCount),
        Triple(RoutingFilterCategory.BLOCK, stringResource(R.string.per_app_proxy_mode_block), blockCount)
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items, key = { it.first }) { (category, label, count) ->
            val isSelected = selectedCategory == category
            val textWithCount = "$label ($count)"
            Surface(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .semantics {
                        this.selected = isSelected
                        this.role = Role.RadioButton
                        this.contentDescription = textWithCount
                    }
            ) {
                Text(
                    text = textWithCount,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PerAppRoutingListItem(
    appName: String,
    packageName: String,
    currentAction: AppRoutingAction,
    onActionChanged: (AppRoutingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val model = remember(packageName) {
            val data = "appicon:$packageName"
            ImageRequest.Builder(context)
                .data(data)
                .fetcherFactory(AppIconFetcher.Factory(context))
                .build()
        }

        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppRoutingActionSelector(
            appName = appName,
            currentAction = currentAction,
            onActionSelected = onActionChanged
        )
    }
}

@Composable
private fun AppRoutingActionSelector(
    appName: String,
    currentAction: AppRoutingAction,
    onActionSelected: (AppRoutingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple(AppRoutingAction.PROXY, R.drawable.ic_routing_proxy_24dp, R.string.per_app_proxy_mode_proxy),
        Triple(AppRoutingAction.DIRECT, R.drawable.ic_routing_direct_24dp, R.string.per_app_proxy_mode_direct),
        Triple(AppRoutingAction.BLOCK, R.drawable.ic_routing_block_24dp, R.string.per_app_proxy_mode_block)
    )

    Surface(
        modifier = modifier
            .width(132.dp)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            actions.forEach { (action, iconRes, labelRes) ->
                val isSelected = currentAction == action
                val actionLabel = stringResource(labelRes)
                val accessibilityLabel = "$appName: $actionLabel"
                Surface(
                    onClick = { onActionSelected(action) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        when (action) {
                            AppRoutingAction.PROXY -> MaterialTheme.colorScheme.primaryContainer
                            AppRoutingAction.DIRECT -> MaterialTheme.colorScheme.secondaryContainer
                            AppRoutingAction.BLOCK -> MaterialTheme.colorScheme.errorContainer
                        }
                    } else {
                        Color.Transparent
                    },
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            this.selected = isSelected
                            this.role = Role.RadioButton
                            this.contentDescription = accessibilityLabel
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = if (isSelected) {
                                when (action) {
                                    AppRoutingAction.PROXY -> MaterialTheme.colorScheme.onPrimaryContainer
                                    AppRoutingAction.DIRECT -> MaterialTheme.colorScheme.onSecondaryContainer
                                    AppRoutingAction.BLOCK -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
