package com.v2ray.ang.ui.perappproxy

import android.app.Application
import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.enums.AppRoutingAction
import com.v2ray.ang.enums.PerAppProxyMode
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.AppSelection
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator

enum class RoutingFilterCategory {
    ALL,
    PROXY,
    DIRECT,
    BLOCK
}

/**
 * ViewModel for PerAppProxy screen.
 * Holds all UI state and business logic for mixed per-app routing.
 */
class PerAppProxyViewModel(application: Application) : BaseViewModel(application) {

    private val _directApps = MutableStateFlow(SettingsManager.getPerAppDirectApps())
    val directApps: StateFlow<Set<String>> = _directApps.asStateFlow()

    private val _blockApps = MutableStateFlow(SettingsManager.getPerAppBlockApps())
    val blockApps: StateFlow<Set<String>> = _blockApps.asStateFlow()

    private val _filterCategory = MutableStateFlow(RoutingFilterCategory.ALL)
    val filterCategory: StateFlow<RoutingFilterCategory> = _filterCategory.asStateFlow()

    private val _displayedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val displayedApps: StateFlow<List<AppInfo>> = _displayedApps.asStateFlow()

    private val _perAppProxyEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY, false)
    )
    val perAppProxyEnabled: StateFlow<Boolean> = _perAppProxyEnabled.asStateFlow()

    // Cached full list for filtering
    private var appsAll: List<AppInfo>? = null
    private var currentQuery = ""
    private var isAppListLoading = false

    val totalCount: Int get() = appsAll?.size ?: 0
    val directCount: Int get() = _directApps.value.size
    val blockCount: Int get() = _blockApps.value.size
    val proxyCount: Int get() = (totalCount - directCount - blockCount).coerceAtLeast(0)

    fun getAppRoutingAction(packageName: String): AppRoutingAction {
        return when {
            _blockApps.value.contains(packageName) -> AppRoutingAction.BLOCK
            _directApps.value.contains(packageName) -> AppRoutingAction.DIRECT
            else -> AppRoutingAction.PROXY
        }
    }

    fun setAppRouting(packageName: String, action: AppRoutingAction) {
        val currentDirect = _directApps.value
        val currentBlock = _blockApps.value
        val newDirect: Set<String>
        val newBlock: Set<String>
        when (action) {
            AppRoutingAction.PROXY -> {
                newDirect = currentDirect - packageName
                newBlock = currentBlock - packageName
            }
            AppRoutingAction.DIRECT -> {
                newDirect = currentDirect + packageName
                newBlock = currentBlock - packageName
            }
            AppRoutingAction.BLOCK -> {
                newDirect = currentDirect - packageName
                newBlock = currentBlock + packageName
            }
        }
        if (newDirect != currentDirect || newBlock != currentBlock) {
            _directApps.value = newDirect
            _blockApps.value = newBlock
            SettingsManager.setPerAppRoutingSets(newDirect, newBlock)
            SettingsChangeManager.makeRestartService()
            updateDisplayedApps()
        }
    }

    fun setFilterCategory(category: RoutingFilterCategory) {
        if (_filterCategory.value != category) {
            _filterCategory.value = category
            updateDisplayedApps()
        }
    }

    // Per‑app proxy master switch
    fun setPerAppProxyEnabled(enabled: Boolean) {
        if (_perAppProxyEnabled.value != enabled) {
            _perAppProxyEnabled.value = enabled
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, enabled)
            SettingsChangeManager.makeRestartService()
        }
    }

    // Load and filter apps
    fun loadApps(context: Context) {
        if (appsAll != null || isAppListLoading) return

        val applicationContext = context.applicationContext
        isAppListLoading = true
        launchLoading {
            try {
                val apps = withContext(Dispatchers.IO) {
                    AppManagerUtil.loadNetworkAppList(applicationContext)
                }
                appsAll = apps
                updateDisplayedApps()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Error loading apps", e)
            } finally {
                isAppListLoading = false
            }
        }
    }

    fun filterApps(query: String) {
        currentQuery = query
        updateDisplayedApps()
    }

    private fun updateDisplayedApps() {
        val apps = appsAll ?: return
        val direct = _directApps.value
        val block = _blockApps.value
        val query = currentQuery

        val filtered = apps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            if (!matchesQuery) return@filter false

            when (_filterCategory.value) {
                RoutingFilterCategory.ALL -> true
                RoutingFilterCategory.PROXY -> !direct.contains(app.packageName) && !block.contains(app.packageName)
                RoutingFilterCategory.DIRECT -> direct.contains(app.packageName)
                RoutingFilterCategory.BLOCK -> block.contains(app.packageName)
            }
        }
        _displayedApps.value = sortApps(filtered)
    }

    private fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        val collator = Collator.getInstance()
        val direct = _directApps.value
        val block = _blockApps.value
        return apps.sortedWith { p1, p2 ->
            val prio1 = when {
                block.contains(p1.packageName) -> 3
                direct.contains(p1.packageName) -> 2
                else -> 1
            }
            val prio2 = when {
                block.contains(p2.packageName) -> 3
                direct.contains(p2.packageName) -> 2
                else -> 1
            }
            when {
                prio1 != prio2 -> prio2.compareTo(prio1)
                p1.isSystemApp && !p2.isSystemApp -> 1
                !p1.isSystemApp && p2.isSystemApp -> -1
                else -> collator.compare(p1.appName, p2.appName)
            }
        }
    }

    // Bulk actions
    fun setAllDisplayed(action: AppRoutingAction) {
        val displayed = _displayedApps.value
        val newDirect = _directApps.value.toMutableSet()
        val newBlock = _blockApps.value.toMutableSet()
        displayed.forEach { app ->
            val pkg = app.packageName
            when (action) {
                AppRoutingAction.PROXY -> {
                    newDirect.remove(pkg)
                    newBlock.remove(pkg)
                }
                AppRoutingAction.DIRECT -> {
                    newDirect.add(pkg)
                    newBlock.remove(pkg)
                }
                AppRoutingAction.BLOCK -> {
                    newDirect.remove(pkg)
                    newBlock.add(pkg)
                }
            }
        }
        _directApps.value = newDirect
        _blockApps.value = newBlock
        SettingsManager.setPerAppRoutingSets(newDirect, newBlock)
        enablePerAppProxyAndRestart()
        updateDisplayedApps()
    }

    fun resetAll() {
        _directApps.value = emptySet()
        _blockApps.value = emptySet()
        SettingsManager.setPerAppRoutingSets(emptySet(), emptySet())
        SettingsChangeManager.makeRestartService()
        updateDisplayedApps()
    }

    fun selectProxyAppAuto(context: Context) {
        val applicationContext = context.applicationContext
        launchLoading {
            val url = AppConfig.ANDROID_PACKAGE_NAME_LIST_URL
            var content = withContext(Dispatchers.IO) {
                HttpUtil.getUrlContent(
                    UrlContentRequest(
                        url = url,
                        timeout = 5000
                    )
                )
            }
            if (content.isNullOrEmpty()) {
                val proxyUsername = SettingsManager.getSocksUsername()
                val proxyPassword = SettingsManager.getSocksPassword()
                val httpPort = SettingsManager.getHttpPort()
                content = withContext(Dispatchers.IO) {
                    HttpUtil.getUrlContent(
                        UrlContentRequest(
                            url = url,
                            timeout = 5000,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword
                        )
                    )
                } ?: ""
            }
            val success = applyProxyAppList(
                content = content,
                context = applicationContext,
                forceGoogleApps = true
            )
            if (success) {
                enablePerAppProxyAndRestart()
            }
        }
    }

    fun importProxyApp(content: String?, context: Context) {
        if (content.isNullOrEmpty()) return

        val newDirect = _directApps.value.toMutableSet()
        val newBlock = _blockApps.value.toMutableSet()

        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val firstLine = lines.firstOrNull()?.lowercase()

        val hasPrefixes = lines.any { it.startsWith("direct:") || it.startsWith("block:") || it.startsWith("proxy:") }
        if (hasPrefixes) {
            lines.forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val act = parts[0].trim().lowercase()
                    val pkg = parts[1].trim()
                    when (act) {
                        "direct" -> {
                            newDirect.add(pkg)
                            newBlock.remove(pkg)
                        }
                        "block" -> {
                            newDirect.remove(pkg)
                            newBlock.add(pkg)
                        }
                        "proxy" -> {
                            newDirect.remove(pkg)
                            newBlock.remove(pkg)
                        }
                    }
                }
            }
        } else {
            val legacyMode = when (firstLine) {
                "true", "bypass" -> PerAppProxyMode.BYPASS
                "false", "proxy" -> PerAppProxyMode.PROXY
                "block" -> PerAppProxyMode.BLOCK
                else -> null
            }
            val pkgLines = if (legacyMode != null) lines.drop(1) else lines
            when (legacyMode) {
                PerAppProxyMode.BLOCK -> {
                    pkgLines.forEach {
                        newBlock.add(it)
                        newDirect.remove(it)
                    }
                }
                PerAppProxyMode.BYPASS -> {
                    pkgLines.forEach {
                        newDirect.add(it)
                        newBlock.remove(it)
                    }
                }
                else -> {
                    pkgLines.forEach {
                        newDirect.remove(it)
                        newBlock.remove(it)
                    }
                }
            }
        }

        _directApps.value = newDirect
        _blockApps.value = newBlock
        SettingsManager.setPerAppRoutingSets(newDirect, newBlock)
        enablePerAppProxyAndRestart()
        updateDisplayedApps()
    }

    fun exportProxyApp(): String {
        return buildString {
            append("v2rayng-per-app-v2")
            _directApps.value.forEach { packageName ->
                append(System.lineSeparator())
                append("direct:$packageName")
            }
            _blockApps.value.forEach { packageName ->
                append(System.lineSeparator())
                append("block:$packageName")
            }
        }
    }

    private suspend fun applyProxyAppList(content: String, context: Context, forceGoogleApps: Boolean): Boolean {
        val installedApps = appsAll ?: return false

        try {
            val proxyAppList = if (content.isEmpty()) {
                withContext(Dispatchers.IO) {
                    Utils.readTextFromAssets(context, "proxy_package_name")
                }
            } else content
            if (proxyAppList.isNullOrEmpty()) return false

            val newBlacklist = withContext(Dispatchers.Default) {
                AppSelection.fromProxyList(
                    packageNames = installedApps.map { it.packageName },
                    proxyAppList = proxyAppList,
                    bypassApps = true,
                    forceGoogleApps = forceGoogleApps
                )
            }
            // In default-proxy mode, apps not needing proxy are set to direct
            val newDirect = newBlacklist.toMutableSet()
            _directApps.value = newDirect
            SettingsManager.setPerAppRoutingSets(newDirect, _blockApps.value)
            updateDisplayedApps()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error selecting proxy app", e)
            return false
        }
        return true
    }

    private fun enablePerAppProxyAndRestart() {
        setPerAppProxyEnabled(true)
        SettingsChangeManager.makeRestartService()
    }
}
