package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.AppUpdate
import com.example.data.update.UpdateCheckResult
import com.example.data.update.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository()
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _availableUpdate = MutableStateFlow<AppUpdate?>(null)
    val availableUpdate: StateFlow<AppUpdate?> = _availableUpdate.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 需要用浏览器打开的下载地址。 */
    private val _openUrlEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openUrlEvents: SharedFlow<String> = _openUrlEvents.asSharedFlow()

    /** 正在探测国内直链线路时为 true，弹窗按钮据此显示「正在连接」。 */
    private val _isResolvingMirror = MutableStateFlow(false)
    val isResolvingMirror: StateFlow<Boolean> = _isResolvingMirror.asStateFlow()

    private var checkJob: Job? = null
    private var mirrorJob: Job? = null

    init {
        checkForUpdate(manual = false)
    }

    fun checkForUpdate() {
        checkForUpdate(manual = true)
    }

    fun dismissUpdate() {
        mirrorJob?.cancel()
        _isResolvingMirror.value = false
        _availableUpdate.value = null
    }

    fun ignoreUpdate() {
        val version = _availableUpdate.value?.version ?: return
        preferences.edit().putString(KEY_IGNORED_VERSION, version).apply()
        dismissUpdate()
        _messages.tryEmit("已忽略版本 $version")
    }

    /** 国内直链：依次探测 gh-proxy 线路，用第一条能连通的线路下载。 */
    fun downloadFromMirror() {
        val update = _availableUpdate.value ?: return
        if (_isResolvingMirror.value) return
        mirrorJob = viewModelScope.launch {
            _isResolvingMirror.value = true
            try {
                val url = repository.resolveMirrorDownload(update)
                if (url != null) {
                    _availableUpdate.value = null
                    _openUrlEvents.emit(url)
                } else {
                    _messages.emit("国内线路暂时无法连接，请改用 GitHub 原站或蓝奏云下载")
                }
            } finally {
                _isResolvingMirror.value = false
            }
        }
    }

    /** GitHub 原站：直接打开 Release 中的安装包地址，不经过加速线路。 */
    fun downloadFromGithub() {
        val update = _availableUpdate.value ?: return
        val url = update.githubDownloadUrl.ifBlank { update.releasePageUrl }
        dismissUpdate()
        _openUrlEvents.tryEmit(url)
    }

    private fun checkForUpdate(manual: Boolean) {
        checkJob?.cancel()
        if (manual) _messages.tryEmit("正在检查更新")
        checkJob = viewModelScope.launch {
            when (val result = repository.check(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.Available -> {
                    val ignoredVersion = preferences.getString(KEY_IGNORED_VERSION, null)
                    if (manual || ignoredVersion != result.update.version) {
                        _availableUpdate.value = result.update
                    }
                }
                UpdateCheckResult.NoUpdate -> {
                    if (manual) _messages.emit("当前已是最新版本 ${BuildConfig.VERSION_NAME}")
                }
                UpdateCheckResult.Unavailable -> {
                    if (manual) _messages.emit("检查失败，请稍后重试")
                }
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "update_preferences"
        const val KEY_IGNORED_VERSION = "ignored_version"
    }
}
