package com.example

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BACKUP_UPDATE_PASSWORD
import com.example.ui.screens.BACKUP_UPDATE_URL
import com.example.ui.screens.DailyHotDashboard
import com.example.ui.screens.copyToClipboard
import com.example.ui.screens.openExternalUrl
import com.example.ui.theme.AppSkin
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.GoldPriceViewModel
import com.example.ui.viewmodel.HotSearchViewModel
import com.example.ui.viewmodel.News60sViewModel
import com.example.ui.viewmodel.OilPriceViewModel
import com.example.ui.viewmodel.UpdateViewModel
import com.example.ui.viewmodel.WeatherAlertViewModel

private const val SETTINGS_PREFS = "app_settings"
private const val KEY_SKIN = "skin"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val prefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE)

    setContent {
      // 皮肤选择会保存，下次打开保持不变
      var skin by remember { mutableStateOf(AppSkin.fromName(prefs.getString(KEY_SKIN, null))) }
      val darkSkin = skin.isDark()

      // 状态栏、导航栏图标颜色跟随当前皮肤，避免深色背景上出现深色图标
      LaunchedEffect(darkSkin) {
        val barStyle =
          if (darkSkin) SystemBarStyle.dark(Color.TRANSPARENT)
          else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
      }

      MyApplicationTheme(skin = skin) {
        val hotViewModel: HotSearchViewModel = viewModel()
        val oilViewModel: OilPriceViewModel = viewModel()
        val goldViewModel: GoldPriceViewModel = viewModel()
        val news60sViewModel: News60sViewModel = viewModel()
        val weatherAlertViewModel: WeatherAlertViewModel = viewModel()
        val updateViewModel: UpdateViewModel = viewModel()
        val availableUpdate by updateViewModel.availableUpdate.collectAsState()
        val isResolvingMirror by updateViewModel.isResolvingMirror.collectAsState()
        LaunchedEffect(updateViewModel) {
          updateViewModel.messages.collect { message ->
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
          }
        }
        // 下载地址统一交给系统浏览器打开
        LaunchedEffect(updateViewModel) {
          updateViewModel.openUrlEvents.collect { url ->
            openExternalUrl(this@MainActivity, url)
          }
        }
        DailyHotDashboard(
          hotViewModel = hotViewModel,
          oilViewModel = oilViewModel,
          goldViewModel = goldViewModel,
          news60sViewModel = news60sViewModel,
          weatherAlertViewModel = weatherAlertViewModel,
          skin = skin,
          onSkinChange = { selected ->
            skin = selected
            prefs.edit().putString(KEY_SKIN, selected.name).apply()
          },
          versionLabel = BuildConfig.VERSION_NAME,
          onCheckUpdate = updateViewModel::checkForUpdate,
          availableUpdate = availableUpdate,
          isResolvingMirror = isResolvingMirror,
          onDismissUpdate = updateViewModel::dismissUpdate,
          onIgnoreUpdate = updateViewModel::ignoreUpdate,
          onMirrorDownload = updateViewModel::downloadFromMirror,
          onGithubDownload = updateViewModel::downloadFromGithub,
          onBackupDownload = {
            updateViewModel.dismissUpdate()
            copyToClipboard(this@MainActivity, BACKUP_UPDATE_PASSWORD)
            openExternalUrl(this@MainActivity, BACKUP_UPDATE_URL)
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
