package com.example.tracker.ui.plan

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.tracker.data.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DISPLAY_SIDE = 1024

/**
 * Menampilkan gambar dari [Uri] (pratinjau di dialog) atau path file String (plan tersimpan).
 * Di-decode di thread IO dan diperkecil, tanpa library tambahan.
 */
@Composable
fun PlanImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, model) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                when (model) {
                    is Uri -> decodeSampledBitmap(context, model, MAX_DISPLAY_SIDE)
                    is String -> decodeSampledBitmap(model, MAX_DISPLAY_SIDE)
                    else -> null
                }?.asImageBitmap()
            }.onFailure { Log.w("PlanImage", "Gagal memuat gambar: $model", it) }.getOrNull()
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}