package app.n_zik.android.components.dialog.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.components.import.ImportDatabase
import app.n_zik.android.components.import.ImportSettings
import timber.log.Timber
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.RestartAppDialog

object ImportBackupDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.import_backup)

    override var isActive: Boolean by mutableStateOf(false)

    @Composable
    override fun DialogBody() {
        val context = LocalContext.current
        var selectedOption by remember { mutableIntStateOf(0) }
        var isBothMode by remember { mutableStateOf(false) }

        // Settings import callback - called after settings import completes
        val importSettings = ImportSettings(context) {
            Timber.tag("ImportBackupDialog").d("Settings import complete, isBothMode: $isBothMode")
            hideDialog()
            RestartAppDialog.showDialog()
        }

        // Database import callback - called after database import completes
        val importDatabase = ImportDatabase(context) {
            Timber.tag("ImportBackupDialog").d("Database import complete, isBothMode: $isBothMode")
            if (isBothMode) {
                // If both mode, launch settings import next
                Timber.tag("ImportBackupDialog").d("Launching settings import (both mode)...")
                importSettings.onShortClick()
            } else {
                // If database only, show restart dialog
                hideDialog()
                RestartAppDialog.showDialog()
            }
        }

        val databaseLabel = stringResource(R.string.database)
        val databaseDescription = stringResource(R.string.import_database_description)
        val settingsLabel = stringResource(R.string.settings)
        val settingsDescription = stringResource(R.string.import_settings_description)
        val bothLabel = stringResource(R.string.import_both)
        val bothDescription = stringResource(R.string.import_both_description)

        val options = listOf(
            Triple(R.drawable.server, databaseLabel, databaseDescription),
            Triple(R.drawable.settings, settingsLabel, settingsDescription),
            Triple(R.drawable.server, bothLabel, bothDescription)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                options.forEachIndexed { index, (_, title, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(uiRoundnessShape())
                            .clickable { selectedOption = index }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == index,
                            onClick = { selectedOption = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorPalette().text,
                                unselectedColor = colorPalette().textSecondary
                            ),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = typography().xs.semiBold,
                                color = colorPalette().text
                            )
                            Text(
                                text = description,
                                style = typography().xxs,
                                color = colorPalette().textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    Timber.tag("ImportBackupDialog").d("Selected option: $selectedOption")
                    when (selectedOption) {
                        0 -> {
                            isBothMode = false
                            importDatabase.onShortClick()
                        }
                        1 -> {
                            isBothMode = false
                            importSettings.onShortClick()
                        }
                        2 -> {
                            isBothMode = true
                            importDatabase.onShortClick()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().accent,
                    contentColor = colorPalette().textSecondary
                ),
                shape = uiRoundnessShape()
            ) {
                Text(
                    text = stringResource(R.string.import_button),
                    style = typography().s.medium
                )
            }
        }
    }
}
