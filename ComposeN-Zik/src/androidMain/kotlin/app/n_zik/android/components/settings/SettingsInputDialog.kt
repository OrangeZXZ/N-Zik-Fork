package app.n_zik.android.components.settings

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import app.n_zik.android.components.dialog.InputDialogConstraints
import app.n_zik.android.components.dialog.TextInputDialog

class SettingsInputDialog private constructor(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>,
    private val titleStr: String,
    private val placeholderStr: String,
    constraint: String,
    private val onDismiss: () -> Unit,
    private val onSetValue: (String) -> Unit
): TextInputDialog(constraint) {

    companion object {
        @Composable
        operator fun invoke(
            title: String,
            initialValue: String = "",
            placeholder: String = "",
            constraint: String = InputDialogConstraints.ALL,
            onDismiss: () -> Unit,
            onSetValue: (String) -> Unit
        ): SettingsInputDialog {
            val instance = SettingsInputDialog(
                remember { mutableStateOf(true) },
                remember(initialValue) { mutableStateOf(TextFieldValue(initialValue)) },
                title,
                placeholder,
                constraint,
                onDismiss,
                onSetValue
            )
            // if isActive becomes false because user dismissed or confirmed
            if (!instance.isActive) {
                onDismiss()
            }
            return instance
        }
    }

    override val keyboardOption: KeyboardOptions = KeyboardOptions.Default
    override val dialogTitle: String
        @Composable
        get() = titleStr

    override var value: TextFieldValue by valueState
    override var isActive: Boolean by activeState

    override fun onSet(newValue: String) {
        super.onSet(newValue)
        if (errorMessage.isNotEmpty()) return
        onSetValue(newValue)
        hideDialog()
    }
}
