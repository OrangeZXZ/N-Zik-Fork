package app.it.fast4x.rimusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import app.n_zik.android.core.coil.ImageCacheFactory
import com.google.common.util.concurrent.ListenableFuture

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val bitmapSize: Int,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            var bitmap = ImageCacheFactory.loadBitmap(uri.toString(), allowHardware = false)
            
            if (bitmap == null && (uri.scheme == android.content.ContentResolver.SCHEME_CONTENT || uri.scheme == android.content.ContentResolver.SCHEME_FILE)) {
                val drawable = ContextCompat.getDrawable(context, app.n_zik.android.R.drawable.ic_launcher_box)
                if (drawable is BitmapDrawable) {
                    bitmap = drawable.bitmap
                } else if (drawable != null) {
                    bitmap = Bitmap.createBitmap(
                        if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else bitmapSize,
                        if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else bitmapSize,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap!!)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }
            
            bitmap ?: error("Could not load image")
        }

}



