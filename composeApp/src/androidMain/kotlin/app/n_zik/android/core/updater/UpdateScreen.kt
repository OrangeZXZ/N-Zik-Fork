@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package app.n_zik.android.core.updater

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.kreate.android.me.knighthat.utils.Repository
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.CheckUpdateState
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import app.it.fast4x.rimusic.ui.styling.ModernBlackColorPalette
import app.it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import app.it.fast4x.rimusic.utils.bold
import app.it.fast4x.rimusic.utils.checkBetaUpdatesKey
import app.it.fast4x.rimusic.utils.checkUpdateStateKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.launch
import app.it.fast4x.rimusic.ui.screens.settings.SettingsDescription
import app.it.fast4x.rimusic.utils.secondary
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.BuildConfig
import app.n_zik.android.R
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber

@Composable
fun UpdateScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val downloadState by UpdateDownloadManager.downloadState.collectAsStateWithLifecycle()
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

    // Update Preferences
    var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Enabled)
    val currentSuffix = Updater.githubRelease?.tagName?.let {
        Updater.extractVersionSuffix(it)
    } ?: Updater.extractVersionSuffix(BuildConfig.VERSION_NAME)
    var checkBetaUpdates by rememberPreference(checkBetaUpdatesKey, currentSuffix == UpdaterConstants.SUFFIX_CHAR_BETA)

    // Handle back press during download â€” let download continue in background
    BackHandler(enabled = downloadState is UpdateDownloadManager.DownloadState.Downloading ||
            downloadState is UpdateDownloadManager.DownloadState.Starting) {
        Toaster.i(R.string.download_cancelled)
        UpdateDownloadManager.cancelDownload(context)
        navController.popBackStack()
    }

    // Cleanup state on exit â€” but NOT if download is active or completed
    DisposableEffect(Unit) {
        onDispose {
            val currentState = UpdateDownloadManager.downloadState.value
            if (currentState is UpdateDownloadManager.DownloadState.Failed) {
                UpdateDownloadManager.resetState()
            }
            // Don't reset Idle, Downloading, Starting or Completed â€” let download persist
        }
    }

    val newVersion = Updater.githubRelease?.tagName?.let { "$it${Updater.getBuildSuffix()}" } ?: ""
    val hasUpdate = Updater.githubRelease != null && Updater.isVersionNewer(Updater.githubRelease?.tagName ?: "", BuildConfig.VERSION_NAME)
    val currentVersion = BuildConfig.VERSION_NAME
    val currentBuildTypeLabel = stringResource(
        when (Updater.extractBuildType(currentVersion)) {
            UpdaterConstants.TYPE_BETA -> R.string.beta_title
            UpdaterConstants.TYPE_MINIFIED -> R.string.minified_title
            else -> R.string.stable_title
        }
    )
    val updateBuildTypeLabel = stringResource(
        when (Updater.extractBuildType(newVersion)) {
            UpdaterConstants.TYPE_BETA -> R.string.beta_title
            UpdaterConstants.TYPE_MINIFIED -> R.string.minified_title
            else -> R.string.stable_title
        }
    )
    val fileSize = try { Updater.build.readableSize } catch (_: Exception) { "" }

    androidx.compose.material3.Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorPalette().background0,
        topBar = {
            AppHeader( navController ).Draw()
        },
        bottomBar = {
            // Floating Action button at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background0.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (val state = downloadState) {
                    is UpdateDownloadManager.DownloadState.Starting,
                    is UpdateDownloadManager.DownloadState.Downloading,
                    is UpdateDownloadManager.DownloadState.Completed -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorPalette().background1),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                if (state is UpdateDownloadManager.DownloadState.Downloading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val downloadStr = if (hasUpdate) {
                                            stringResource(R.string.downloading_update)
                                        } else {
                                            val extraInfo = if (fileSize.isNotEmpty()) "$currentBuildTypeLabel - $fileSize" else currentBuildTypeLabel
                                            stringResource(R.string.downloading_actual_version, currentVersion, extraInfo)
                                        }
                                        BasicText(
                                            text = downloadStr,
                                            style = typography().s.bold.copy(color = colorPalette().text)
                                        )
                                        BasicText(
                                            text = "${(state.progress * 100).toInt()}%",
                                            style = typography().s.copy(color = colorPalette().accent)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.material3.LinearWavyProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = colorPalette().accent,
                                        trackColor = colorPalette().background2
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    Toaster.i(R.string.download_cancelled)
                                                    UpdateDownloadManager.cancelDownload(context)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            BasicText(
                                                text = stringResource(R.string.cancel),
                                                style = typography().s.semiBold.copy(color = colorPalette().red)
                                            )
                                        }
                                    }
                                } else if (state is UpdateDownloadManager.DownloadState.Starting) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicText(
                                            text = stringResource(R.string.starting),
                                            style = typography().s.bold.copy(color = colorPalette().text)
                                        )
                                        CircularWavyProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = colorPalette().accent,
                                        )
                                    }
                                } else if (state is UpdateDownloadManager.DownloadState.Completed) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                UpdateDownloadManager.installApk(context, state.filePath)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = colorPalette().accent),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.checkmark),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            BasicText(
                                                text = stringResource(R.string.install),
                                                style = typography().s.bold.copy(color = Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (state is UpdateDownloadManager.DownloadState.Failed) {
                                LaunchedEffect(state) {
                                    Toaster.e(state.error)
                                    UpdateDownloadManager.resetState()
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (hasUpdate) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                UpdateDownloadManager.startDownload(
                                                    context,
                                                    Updater.build.downloadUrl,
                                                    newVersion
                                                )
                                            },
                                        colors = CardDefaults.cardColors(containerColor = colorPalette().accent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.download),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                BasicText(
                                                    text = if (state is UpdateDownloadManager.DownloadState.Failed)
                                                        stringResource(R.string.retry)
                                                    else
                                                        stringResource(R.string.download),
                                                    style = typography().s.bold.copy(color = Color.White)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                Toaster.i(R.string.checking_for_updates)
                                                Updater.checkForUpdate(true, checkBetaUpdates, showDialog = false)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = colorPalette().background1),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colorPalette().accent.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.update),
                                                    contentDescription = null,
                                                    tint = colorPalette().accent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                BasicText(
                                                    text = stringResource(R.string.check_update),
                                                    style = typography().s.semiBold.copy(color = colorPalette().accent)
                                                )
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val rawVersion = if (hasUpdate) newVersion else currentVersion
                                            val cleanVersion = rawVersion.removePrefix("v")
                                            val tag = if (Updater.extractBuildType(cleanVersion) == UpdaterConstants.TYPE_BETA) {
                                                "v$cleanVersion"
                                            } else {
                                                "v${cleanVersion.substringBefore('-')}"
                                            }
                                            uriHandler.openUri("${Repository.REPO_URL}/releases/tag/$tag")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = colorPalette().background1),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorPalette().textSecondary.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier 
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(R.drawable.github_icon),
                                                contentDescription = null,
                                                tint = colorPalette().text,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            BasicText(
                                                text = "GitHub",
                                                style = typography().s.semiBold.copy(color = colorPalette().text)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                // Top Card (Up to date or Update available)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                        animationSpec = tween(600),
                        initialScale = 0.8f
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                    Color(0xFF1A1A1A)
                                } else {
                                    colorPalette().background1
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                
                                // Settings Menu positioned at TopEnd
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                    var showMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ellipsis_vertical),
                                            contentDescription = stringResource(R.string.menu),
                                            tint = colorPalette().text,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    val menu = app.it.fast4x.rimusic.ui.components.themed.DropdownMenu(
                                        expanded = showMenu,
                                        modifier = Modifier.background(colorPalette().background0.copy(0.90f)),
                                        onDismissRequest = { showMenu = false }
                                    )
                                    val isMinified = Updater.extractVersionSuffix(BuildConfig.VERSION_NAME) == UpdaterConstants.SUFFIX_CHAR_MINIFIED
                                    menu.add(
                                        app.it.fast4x.rimusic.ui.components.themed.DropdownMenu.Item(
                                            iconId = R.drawable.shield_checkmark,
                                            customText = "${stringResource(R.string.beta_updates)}: ${if (isMinified) stringResource(R.string.off) else if (checkBetaUpdates) stringResource(R.string.on) else stringResource(R.string.off)}",
                                            enabled = !isMinified
                                        ) {
                                            if (!isMinified) {
                                                checkBetaUpdates = !checkBetaUpdates
                                            }
                                        }
                                    )
                                    val stateStr = when(checkUpdateState) {
                                        CheckUpdateState.Enabled -> stringResource(R.string.on)
                                        CheckUpdateState.Ask -> stringResource(R.string.ask)
                                        CheckUpdateState.Disabled -> stringResource(R.string.off)
                                    }
                                    menu.add(
                                        app.it.fast4x.rimusic.ui.components.themed.DropdownMenu.Item(
                                            iconId = R.drawable.update,
                                            customText = "${stringResource(R.string.enable_check_for_update)}: $stateStr"
                                        ) {
                                            checkUpdateState = when (checkUpdateState) {
                                                CheckUpdateState.Enabled -> CheckUpdateState.Ask
                                                CheckUpdateState.Ask -> CheckUpdateState.Disabled
                                                CheckUpdateState.Disabled -> CheckUpdateState.Enabled
                                            }
                                        }
                                    )
                                    val currentVersionStr = BuildConfig.VERSION_NAME
                                    val tagVersion = if (currentVersionStr.endsWith(UpdaterConstants.SUFFIX_BETA)) currentVersionStr else currentVersionStr.removePrefix(UpdaterConstants.PREFIX_VERSION).substringBefore("-")
                                    val buildType = try { Updater.extractBuildType(currentVersionStr) } catch(e: Exception) { UpdaterConstants.TYPE_FULL }
                                    val apkName = "${BuildConfig.APP_NAME}-$buildType.apk"
                                    val currentDownloadUrl = "${Repository.RELEASE_DOWNLOAD_URL}$tagVersion/$apkName"
                                    
                                    val downloadText = "${stringResource(R.string.redownload_update)} ($currentVersionStr)"
                                    menu.add(
                                        app.it.fast4x.rimusic.ui.components.themed.DropdownMenu.Item(
                                            iconId = R.drawable.download,
                                            customText = downloadText
                                        ) {
                                            showMenu = false
                                            try {
                                                Timber.d("Redownload URL: $currentDownloadUrl")
                                                UpdateDownloadManager.startDownload(
                                                    context = context,
                                                    apkUrl = currentDownloadUrl,
                                                    version = currentVersionStr
                                                )
                                                Updater.fetchCurrentFastlaneChangelog()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toaster.w(R.string.update_not_available_yet)
                                            }
                                        }
                                    )
                                    menu.add(
                                        app.it.fast4x.rimusic.ui.components.themed.DropdownMenu.Item(
                                            R.drawable.trash,
                                            R.string.update_cache_cleared
                                        ) {
                                            showMenu = false
                                            UpdateDownloadManager.clearCache(context)
                                            Toaster.s(R.string.update_cache_cleared)
                                        }
                                    )
                                    menu.Draw()
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val isReinstalling = downloadState !is UpdateDownloadManager.DownloadState.Idle && UpdateDownloadManager.downloadingVersion == currentVersion
                                    if (hasUpdate || isReinstalling) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(colorPalette().accent.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isReinstalling) R.drawable.download else R.drawable.arrow_up),
                                                contentDescription = null,
                                                tint = colorPalette().accent,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        BasicText(
                                            text = if (isReinstalling) stringResource(R.string.reinstalling_update) else "$updateBuildTypeLabel ${stringResource(R.string.update_available)}",
                                            style = typography().l.bold.copy(color = colorPalette().text)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        BasicText(
                                            text = if (isReinstalling) currentVersion else newVersion,
                                            style = typography().s.copy(color = colorPalette().textSecondary)
                                        )
                                        if (fileSize.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            BasicText(
                                                text = stringResource(R.string.update_file_size, fileSize),
                                                style = typography().xs.copy(color = colorPalette().textSecondary)
                                            )
                                        }
                                    } else if (Updater.githubRelease != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(colorPalette().accent.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.checkmark),
                                                contentDescription = null,
                                                tint = colorPalette().accent,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        BasicText(
                                            text = stringResource(R.string.up_to_date),
                                            style = typography().l.bold.copy(color = colorPalette().text)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        BasicText(
                                            text = "${UpdaterConstants.PREFIX_VERSION}$currentVersion",
                                            style = typography().s.copy(color = colorPalette().textSecondary)
                                        )
                                        // No file size displayed when up to date
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(colorPalette().accent.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.information),
                                                contentDescription = null,
                                                tint = colorPalette().accent,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        BasicText(
                                            text = BuildConfig.APP_NAME,
                                            style = typography().l.bold.copy(color = colorPalette().text)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val stateStr = when(checkUpdateState) {
                                            app.it.fast4x.rimusic.enums.CheckUpdateState.Enabled -> stringResource(R.string.auto_update_enabled)
                                            app.it.fast4x.rimusic.enums.CheckUpdateState.Ask -> stringResource(R.string.auto_update_ask)
                                            app.it.fast4x.rimusic.enums.CheckUpdateState.Disabled -> stringResource(R.string.auto_update_disabled)
                                        }
                                        BasicText(
                                            text = "${UpdaterConstants.PREFIX_VERSION}$currentVersion â€¢ $stateStr",
                                            style = typography().s.copy(color = colorPalette().textSecondary)
                                        )
                                    }
                                    val lastCheckTime by rememberPreference(app.it.fast4x.rimusic.utils.lastUpdateCheckKey, 0L)
                                    val sdf = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
                                    val lastCheckStr = if (lastCheckTime > 0) sdf.format(java.util.Date(lastCheckTime)) else stringResource(R.string.never_checked)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BasicText(
                                        text = if (lastCheckTime > 0) stringResource(R.string.last_check, lastCheckStr) else stringResource(R.string.never_checked),
                                        style = typography().xxs.secondary.copy(color = colorPalette().textSecondary.copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Changelog Section
                val currentChangelog = remember {
                    try {
                        appContext().resources
                            .openRawResource(R.raw.release_notes)
                            .bufferedReader(Charsets.UTF_8)
                            .readText()
                    } catch (e: Exception) { "" }
                }
                val isReinstalling = downloadState !is UpdateDownloadManager.DownloadState.Idle && UpdateDownloadManager.downloadingVersion == currentVersion
                val changelogTextToDisplay = if (isReinstalling || !hasUpdate) {
                    if (!Updater.currentFastlaneChangelog.isNullOrBlank()) {
                        Updater.currentFastlaneChangelog!!
                    } else {
                        currentChangelog
                    }
                } else {
                    if (!Updater.latestFastlaneChangelog.isNullOrBlank()) {
                        Updater.latestFastlaneChangelog!!
                    } else if (!Updater.githubRelease?.body.isNullOrBlank()) {
                        Updater.githubRelease!!.body
                    } else {
                        currentChangelog
                    }
                }

                if (changelogTextToDisplay.isNotBlank() || Updater.isFetchingFastlane) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                            animationSpec = tween(800),
                            initialScale = 0.9f
                        )
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (colorPalette() === PureBlackColorPalette || colorPalette() === ModernBlackColorPalette || colorPaletteMode == ColorPaletteMode.PitchBlack) {
                                    Color(0xFF1A1A1A)
                                } else {
                                    colorPalette().background1
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.sparkles),
                                        contentDescription = null,
                                        tint = colorPalette().accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicText(
                                        text = stringResource(R.string.whats_new_in, if (hasUpdate && !isReinstalling) newVersion else "${UpdaterConstants.PREFIX_VERSION}$currentVersion"),
                                        style = typography().m.bold.copy(color = colorPalette().text)
                                    )
                                }
                                if (Updater.isFetchingFastlane) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularWavyProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = colorPalette().accent,
                                        )
                                    }
                                } else {
                                    ChangelogCard(
                                        changelogText = changelogTextToDisplay,
                                        colorPaletteMode = colorPaletteMode
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun parseChangelogText(text: String): List<Pair<String, List<String>>> {
    val sections = mutableListOf<Pair<String, List<String>>>()
    var currentTitle: String? = null
    val currentChanges = mutableListOf<String>()

    fun packSection() {
        if (currentTitle != null) {
            sections.add(currentTitle!! to currentChanges.toList())
            currentChanges.clear()
        }
    }

    text.lines().forEach { line ->
        when {
            line.endsWith(":") -> {
                packSection()
                currentTitle = line.removeSuffix(":")
            }
            line.trim().startsWith("-") -> {
                if (line.isNotBlank())
                    currentChanges.add(line.trim())
            }
        }
    }
    packSection()

    if (sections.isEmpty() && currentChanges.isNotEmpty()) {
        val otherTitle = app.it.fast4x.rimusic.appContext().getString(R.string.other)
        sections.add(otherTitle to currentChanges.toList())
    }

    return sections
}

@Composable
fun ChangelogCard(changelogText: String, colorPaletteMode: ColorPaletteMode) {
    val sections = remember(changelogText) { parseChangelogText(changelogText) }
    if (sections.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { section ->
            var expanded by remember { androidx.compose.runtime.mutableStateOf(true) }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(
                                when (section.first.lowercase()) {
                                    UpdaterConstants.CHANGELOG_NEW -> R.drawable.add
                                    UpdaterConstants.CHANGELOG_CHANGED -> R.drawable.title_edit
                                    UpdaterConstants.CHANGELOG_IMPROVED -> R.drawable.refresh_circle
                                    UpdaterConstants.CHANGELOG_FIXED -> R.drawable.alert
                                    else -> R.drawable.information
                                }
                            ),
                            contentDescription = null,
                            tint = colorPalette().accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicText(
                            text = section.first,
                            style = typography().s.semiBold.copy(color = colorPalette().text)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(if (expanded) R.drawable.chevron_up else R.drawable.chevron_down),
                            contentDescription = null,
                            tint = colorPalette().textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            section.second.forEach { change ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                when (section.first.lowercase()) {
                                                    UpdaterConstants.CHANGELOG_NEW -> Color(0xFF4CAF50)
                                                    UpdaterConstants.CHANGELOG_CHANGED -> Color(0xFFFF9800)
                                                    UpdaterConstants.CHANGELOG_IMPROVED -> Color(0xFF2196F3)
                                                    UpdaterConstants.CHANGELOG_FIXED -> Color(0xFFF44336)
                                                    else -> colorPalette().accent
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    BasicText(
                                        text = change.removePrefix("- "),
                                        style = typography().xs.copy(color = colorPalette().text),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
