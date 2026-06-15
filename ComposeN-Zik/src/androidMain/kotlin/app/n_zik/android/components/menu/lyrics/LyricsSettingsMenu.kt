package app.n_zik.android.components.menu.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.R
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.n_zik.android.components.menu.GridMenu
import app.n_zik.android.components.menu.ListMenu
import app.n_zik.android.colorPalette
import app.it.fast4x.rimusic.ui.components.themed.Menu as ThemedMenu
import app.it.fast4x.rimusic.ui.components.themed.MenuEntry
import app.it.fast4x.rimusic.utils.landscapeControlsKey
import app.n_zik.android.enums.lyrics.LyricsAlignment
import app.it.fast4x.rimusic.utils.lyricsAlignmentKey
import app.n_zik.android.enums.lyrics.LyricsFontSize
import app.it.fast4x.rimusic.utils.lyricsFontSizeKey
import app.n_zik.android.enums.lyrics.LyricsColor
import app.it.fast4x.rimusic.utils.lyricsColorKey
import app.n_zik.android.enums.lyrics.LyricsOutline
import app.it.fast4x.rimusic.utils.lyricsOutlineKey
import app.it.fast4x.rimusic.enums.Languages
import app.it.fast4x.rimusic.utils.otherLanguageAppKey
import app.it.fast4x.rimusic.enums.Romanization
import app.it.fast4x.rimusic.utils.romanizationKey
import app.it.fast4x.rimusic.utils.showSecondLineKey
import app.it.fast4x.rimusic.utils.lyricsSizeAnimateKey
import app.n_zik.android.enums.lyrics.LyricsHighlight
import app.it.fast4x.rimusic.utils.lyricsHighlightKey
import app.n_zik.android.enums.lyrics.LyricsBackground
import app.it.fast4x.rimusic.utils.lyricsBackgroundKey
import app.it.fast4x.rimusic.utils.isShowingSynchronizedLyricsKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.languageDestinationName

@UnstableApi
class LyricsSettingsMenu private constructor(
    private val isLandscape: Boolean,
    private val translateEnabled: MutableState<Boolean>,
    private val isLyricsNotNull: Boolean,
    private val onShowLyricsSizeDialog: () -> Unit,
    private val onEditLyrics: () -> Unit,
    private val onCopyLyrics: () -> Unit,
    private val onSearchLyricsOnline: () -> Unit,
    private val onFetchLyricsAgain: () -> Unit,
    private val onPickFromLrcLib: () -> Unit,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(
            isLandscape: Boolean,
            translateEnabled: MutableState<Boolean>,
            isLyricsNotNull: Boolean,
            onShowLyricsSizeDialog: () -> Unit,
            onEditLyrics: () -> Unit,
            onCopyLyrics: () -> Unit,
            onSearchLyricsOnline: () -> Unit,
            onFetchLyricsAgain: () -> Unit,
            onPickFromLrcLib: () -> Unit
        ): LyricsSettingsMenu =
            LyricsSettingsMenu(
                isLandscape = isLandscape,
                translateEnabled = translateEnabled,
                isLyricsNotNull = isLyricsNotNull,
                onShowLyricsSizeDialog = onShowLyricsSizeDialog,
                onEditLyrics = onEditLyrics,
                onCopyLyrics = onCopyLyrics,
                onSearchLyricsOnline = onSearchLyricsOnline,
                onFetchLyricsAgain = onFetchLyricsAgain,
                onPickFromLrcLib = onPickFromLrcLib,
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List)
            )
    }

    private lateinit var buttons: List<Button>
    override var menuStyle: MenuStyle by styleState

    @Composable
    override fun ListMenu() = ListMenu.Menu {
        buttons.forEach {
            if (it is MenuIcon)
                it.ListMenuItem()
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu {
        items(buttons, Button::hashCode) {
            if (it is MenuIcon)
                it.GridMenuItem()
        }
    }

    @Composable
    private fun SubMenuComponent(items: List<MenuIcon>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            if (menuStyle == MenuStyle.List) {
                ListMenu.Menu {
                    items.forEach { it.ListMenuItem() }
                }
            } else {
                GridMenu.Menu {
                    items(items, key = { it.hashCode() }) { it.GridMenuItem() }
                }
            }
        }
    }

    @Composable
    override fun MenuComponent() {
        var landscapeControls by rememberPreference(landscapeControlsKey, true)
        var lyricsAlignment by rememberPreference(lyricsAlignmentKey, LyricsAlignment.Center)
        var fontSize by rememberPreference(lyricsFontSizeKey, LyricsFontSize.Medium)
        var lyricsColor by rememberPreference(lyricsColorKey, LyricsColor.Thememode)
        var lyricsOutline by rememberPreference(lyricsOutlineKey, LyricsOutline.None)
        var romanization by rememberPreference(romanizationKey, Romanization.Off)
        var showSecondLine by rememberPreference(showSecondLineKey, false)
        var lyricsSizeAnimate by rememberPreference(lyricsSizeAnimateKey, false)
        var lyricsHighlight by rememberPreference(lyricsHighlightKey, LyricsHighlight.None)
        var lyricsBackground by rememberPreference(lyricsBackgroundKey, LyricsBackground.Black)
        var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, false)
        val showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, true)
        var otherLanguageApp by rememberPreference(otherLanguageAppKey, Languages.English)

        buttons = mutableListOf<Button>().apply {
            if (isLandscape && !showlyricsthumbnail) {
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = if (landscapeControls) R.drawable.checkmark else R.drawable.play
                    override val messageId: Int = R.string.toggle_controls_landscape
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.hide()
                        landscapeControls = !landscapeControls
                    }
                    override fun onLongClick() {}
                })
            }

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.text
                override val messageId: Int = R.string.lyricsalignment
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.display {
                        SubMenuComponent(listOf(
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = R.drawable.arrow_left
                                override val messageId = R.string.direction_left
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); lyricsAlignment = LyricsAlignment.Left }
                                override fun onLongClick() {}
                            },
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = R.drawable.arrow_down
                                override val messageId = R.string.center
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); lyricsAlignment = LyricsAlignment.Center }
                                override fun onLongClick() {}
                            },
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = R.drawable.arrow_right
                                override val messageId = R.string.direction_right
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); lyricsAlignment = LyricsAlignment.Right }
                                override fun onLongClick() {}
                            }
                        ))
                    }
                }
                override fun onLongClick() {}
            })

            if (!showlyricsthumbnail) {
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.text
                    override val messageId: Int = R.string.lyrics_size
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.display {
                            SubMenuComponent(listOf(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.text
                                    override val messageId = R.string.light
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); fontSize = LyricsFontSize.Light }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.text
                                    override val messageId = R.string.medium
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); fontSize = LyricsFontSize.Medium }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.text
                                    override val messageId = R.string.heavy
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); fontSize = LyricsFontSize.Heavy }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.text
                                    override val messageId = R.string.large
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); fontSize = LyricsFontSize.Large }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.text
                                    override val messageId = R.string.custom
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId) + " (" + stringResource(R.string.lyricsSizeSecondary) + ")"
                                    override fun onShortClick() { menuState.hide(); fontSize = LyricsFontSize.Custom }
                                    override fun onLongClick() { onShowLyricsSizeDialog() }
                                }
                            ))
                        }
                    }
                    override fun onLongClick() {}
                })

                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.droplet
                    override val messageId: Int = R.string.lyricscolor
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.display {
                            SubMenuComponent(listOf(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.theme
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsColor = LyricsColor.Thememode }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.white
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsColor = LyricsColor.White }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.black
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsColor = LyricsColor.Black }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.accent
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsColor = LyricsColor.Accent }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.fluidrainbow
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsColor = LyricsColor.FluidRainbow }
                                    override fun onLongClick() {}
                                }
                            ))
                        }
                    }
                    override fun onLongClick() {}
                })

                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.horizontal_bold_line
                    override val messageId: Int = R.string.lyricsoutline
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.display {
                            val outlineItems = mutableListOf<MenuIcon>(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.close
                                    override val messageId = R.string.none
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.None }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_bold_line
                                    override val messageId = R.string.theme
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.Thememode }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_bold_line
                                    override val messageId = R.string.white
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.White }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_bold_line
                                    override val messageId = R.string.black
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.Black }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.fluidrainbow
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.Rainbow }
                                    override fun onLongClick() {}
                                }
                            )
                            if (isShowingSynchronizedLyrics) {
                                outlineItems.add(
                                    object : MenuIcon, Descriptive, Clickable {
                                        override val iconId = R.drawable.droplet
                                        override val messageId = R.string.glow
                                        @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                        override fun onShortClick() { menuState.hide(); lyricsOutline = LyricsOutline.Glow }
                                        override fun onLongClick() {}
                                    }
                                )
                            }
                            SubMenuComponent(outlineItems)
                        }
                    }
                    override fun onLongClick() {}
                })
            }

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.translate
                override val messageId: Int = R.string.translate_to
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId, languageDestinationName(otherLanguageApp))
                override fun onShortClick() {
                    menuState.hide()
                    translateEnabled.value = true
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.translate
                override val messageId: Int = R.string.translate_to_other_language
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.display {
                        app.n_zik.android.components.menu.lyrics.LanguagesListMenu(
                            translateEnabled = translateEnabled
                        ).MenuComponent()
                    }
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = if (romanization == Romanization.Original || romanization == Romanization.Translated || romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text
                override val messageId: Int = R.string.toggle_romanization
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.display {
                        val romItems = mutableListOf<MenuIcon>(
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = if (romanization == Romanization.Off) R.drawable.checkmark else R.drawable.text
                                override val messageId = R.string.turn_off
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); romanization = Romanization.Off }
                                override fun onLongClick() {}
                            },
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = if (romanization == Romanization.Original || (romanization == Romanization.Both && !showSecondLine)) R.drawable.checkmark else R.drawable.text
                                override val messageId = R.string.original_lyrics
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); romanization = Romanization.Original }
                                override fun onLongClick() {}
                            },
                            object : MenuIcon, Descriptive, Clickable {
                                override val iconId = if (romanization == Romanization.Translated) R.drawable.checkmark else R.drawable.text
                                override val messageId = R.string.translated_lyrics
                                @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                override fun onShortClick() { menuState.hide(); romanization = Romanization.Translated }
                                override fun onLongClick() {}
                            }
                        )
                        if (showSecondLine) {
                            romItems.add(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = if (romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text
                                    override val messageId = R.string.both
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); romanization = Romanization.Both }
                                    override fun onLongClick() {}
                                }
                            )
                        }
                        SubMenuComponent(romItems)
                    }
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = if (showSecondLine) R.drawable.checkmark else R.drawable.close
                override val messageId: Int = R.string.showsecondline
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.hide()
                    showSecondLine = !showSecondLine
                }
                override fun onLongClick() {}
            })

            if (!showlyricsthumbnail && isShowingSynchronizedLyrics) {
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = if (lyricsSizeAnimate) R.drawable.checkmark else R.drawable.close
                    override val messageId: Int = R.string.lyricsanimate
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.hide()
                        lyricsSizeAnimate = !lyricsSizeAnimate
                    }
                    override fun onLongClick() {}
                })
            }

            if (!showlyricsthumbnail) {
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.horizontal_bold_line_rounded
                    override val messageId: Int = R.string.highlight
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.display {
                            SubMenuComponent(listOf(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_straight_line
                                    override val messageId = R.string.none
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsHighlight = LyricsHighlight.None }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_straight_line
                                    override val messageId = R.string.white
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsHighlight = LyricsHighlight.White }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.horizontal_straight_line
                                    override val messageId = R.string.black
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsHighlight = LyricsHighlight.Black }
                                    override fun onLongClick() {}
                                }
                            ))
                        }
                    }
                    override fun onLongClick() {}
                })

                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.droplet
                    override val messageId: Int = R.string.lyricsbackground
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId)
                    override fun onShortClick() {
                        menuState.display {
                            SubMenuComponent(listOf(
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.none
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsBackground = LyricsBackground.None }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.white
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsBackground = LyricsBackground.White }
                                    override fun onLongClick() {}
                                },
                                object : MenuIcon, Descriptive, Clickable {
                                    override val iconId = R.drawable.droplet
                                    override val messageId = R.string.black
                                    @get:Composable override val menuIconTitle get() = stringResource(messageId)
                                    override fun onShortClick() { menuState.hide(); lyricsBackground = LyricsBackground.Black }
                                    override fun onLongClick() {}
                                }
                            ))
                        }
                    }
                    override fun onLongClick() {}
                })
            }

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.time
                override val messageId: Int = R.string.show
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId) + " " + if (isShowingSynchronizedLyrics) stringResource(R.string.unsynchronized_lyrics) else stringResource(R.string.synchronized_lyrics)
                override fun onShortClick() {
                    menuState.hide()
                    isShowingSynchronizedLyrics = !isShowingSynchronizedLyrics
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.title_edit
                override val messageId: Int = R.string.edit_lyrics
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.hide()
                    onEditLyrics()
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.copy
                override val messageId: Int = R.string.copy_lyrics
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.hide()
                    onCopyLyrics()
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.search
                override val messageId: Int = R.string.search_lyrics_online
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override fun onShortClick() {
                    menuState.hide()
                    onSearchLyricsOnline()
                }
                override fun onLongClick() {}
            })

            add(object : MenuIcon, Descriptive, Clickable {
                override val iconId: Int = R.drawable.sync
                override val messageId: Int = R.string.fetch_lyrics_again
                @get:Composable
                override val menuIconTitle: String get() = stringResource(messageId)
                override val isEnabled: Boolean = isLyricsNotNull
                override fun onShortClick() {
                    if (isEnabled) {
                        menuState.hide()
                        onFetchLyricsAgain()
                    }
                }
                override fun onLongClick() {}
            })

            if (isShowingSynchronizedLyrics) {
                add(object : MenuIcon, Descriptive, Clickable {
                    override val iconId: Int = R.drawable.sync
                    override val messageId: Int = R.string.pick_from
                    @get:Composable
                    override val menuIconTitle: String get() = stringResource(messageId) + " LrcLib.net"
                    override fun onShortClick() {
                        menuState.hide()
                        onPickFromLrcLib()
                    }
                    override fun onLongClick() {}
                })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}

