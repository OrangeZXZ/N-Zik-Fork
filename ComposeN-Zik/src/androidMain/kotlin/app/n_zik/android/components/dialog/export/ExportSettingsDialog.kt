package app.n_zik.android.components.dialog.export

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import app.n_zik.android.BuildConfig
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import app.it.fast4x.rimusic.utils.preferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ExportSettingsDialog private constructor(
    private val launcher: ManagedActivityResultLauncher<String, Uri?>,
    private val context: Context
) {
    companion object {
        private fun onExport(
            uri: Uri,
            context: Context
        ) = CoroutineScope( Dispatchers.IO ).launch {
            try {
                Timber.tag("ExportSettingsDialog").d("Starting settings export...")
                val entries: List<Triple<String, String, Any>> = context.preferences
                    .all
                    .map {
                        val value = it.value ?: Unit
                        val type = value::class.simpleName ?: "null"
                        Triple( type, it.key, value )
                    }
                    .filter { it.first != "null" && it.third !== Unit }

                Timber.tag("ExportSettingsDialog").d("Found ${entries.size} settings entries")

                context.contentResolver
                    .openOutputStream( uri )
                    ?.use { outStream ->
                        csvWriter().open( outStream ) {
                            writeRow( "Type", "Key", "Value" )
                            flush()
                            entries.forEach {
                                writeRow( it.first, it.second, it.third )
                            }
                            close()
                        }
                        Timber.tag("ExportSettingsDialog").d("Settings export complete")
                    } ?: Timber.tag("ExportSettingsDialog").w("Failed to open output stream")
            } catch (e: Exception) {
                Timber.tag("ExportSettingsDialog").e(e, "Settings export failed")
            }
        }

        @Composable
        operator fun invoke( context: Context ): ExportSettingsDialog =
            ExportSettingsDialog(
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument( "text/csv" )
                ) { uri ->
                    Timber.tag("ExportSettingsDialog").d("File picker callback received, uri: $uri")
                    uri ?: return@rememberLauncherForActivityResult
                    onExport( uri, context )
                },
                context
            )
    }

    fun export() {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val fileName = "${BuildConfig.APP_NAME} $date Settings"
        Timber.tag("ExportSettingsDialog").d("Launching file picker with name: $fileName.csv")
        launcher.launch("$fileName.csv")
    }
}
