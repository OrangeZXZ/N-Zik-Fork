package app.n_zik.android.enums

import androidx.annotation.StringRes
import app.n_zik.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class PlayerSwipeSensitivity(
    @field:StringRes override val textId: Int,
    val threshold: Float
): TextView {

    Low(R.string.ps_low, 150f),
    Medium(R.string.ps_medium, 75f),
    High(R.string.ps_high, 40f),
    VeryHigh(R.string.ps_very_high, 15f);
}
