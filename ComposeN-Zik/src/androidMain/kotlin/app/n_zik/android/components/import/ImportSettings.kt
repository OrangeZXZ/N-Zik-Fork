package app.n_zik.android.components.import

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastForEach
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.n_zik.android.components.ImportFromFile
import app.n_zik.android.components.dialog.RestartAppDialog
import timber.log.Timber
import java.io.InputStream

class ImportSettings private constructor(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): ImportFromFile(launcher) {

    companion object {
        fun onImport( context: Context, inStream: InputStream ) {
            Timber.tag("ImportSettings").d("Starting settings import...")
            val rows = csvReader().readAllWithHeader( inStream )
            Timber.tag("ImportSettings").d("Read ${rows.size} rows from CSV")
            rows.fastForEach { row ->
                val type = row["Type"] ?: ""
                val key = row["Key"] ?: ""
                val value = row["Value"] ?: ""
                Timber.tag("ImportSettings").d("Processing row: type=$type, key=$key")

                val editor = context.preferences.edit()
                when( type.lowercase() ) {
                    "string" -> editor.putString( key, value )
                    "int" -> editor.putInt( key, value.toInt() )
                    "long" -> editor.putLong( key, value.toLong() )
                    "float" -> editor.putFloat( key, value.toFloat() )
                    "boolean" -> editor.putBoolean( key, value.toBoolean() )
                }
                editor.commit()
            }
            Timber.tag("ImportSettings").d("Settings import complete")
        }

        @Composable
        operator fun invoke( context: Context ): ImportSettings =
            ImportSettings(
                rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    Timber.tag("ImportSettings").d("File picker callback received, uri: $uri")
                    uri ?: return@rememberLauncherForActivityResult

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            context.contentResolver
                                   .openInputStream( uri )
                                   ?.use { inStream ->
                                       onImport( context, inStream )
                                   } ?: Timber.tag("ImportSettings").w("Failed to open input stream")

                            RestartAppDialog.showDialog()
                        } catch (e: Exception) {
                            Timber.tag("ImportSettings").e(e, "Import failed")
                        }
                    }
                }
            )
    }

    override val supportedMimes: Array<String> = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}
