package app.n_zik.android.extensions.nextvisualizer.painters.misc

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import app.n_zik.android.extensions.nextvisualizer.painters.Painter
import app.n_zik.android.extensions.nextvisualizer.utils.VisualizerHelper

class SimpleText(
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE },
    var text: String = "",
    //
    var x: Float = 100f,
    var y: Float = 100f
    ) : Painter() {

    override fun calc(helper: VisualizerHelper) {
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.drawText(text, x, y, paint)
    }
}


