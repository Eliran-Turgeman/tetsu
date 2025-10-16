package com.example.workoutlogger.feature.share.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.exifinterface.media.ExifInterface
import com.example.workoutlogger.feature.share.data.AspectPreset
import com.example.workoutlogger.feature.share.data.ImageFormat
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

object ImageExporter {

    fun createExportBitmap(aspect: AspectPreset): ImageBitmap =
        ImageBitmap(aspect.width, aspect.height)

    fun exportToCache(
        context: Context,
        bitmap: ImageBitmap,
        format: ImageFormat
    ): Uri {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US))
        val extension = if (format == ImageFormat.PNG) "png" else "jpg"
        val file = File(cacheDir, "consistency_${timestamp}.$extension")
        FileOutputStream(file).use { out ->
            val androidBitmap = bitmap.asAndroidBitmap()
            val compressFormat = when (format) {
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            }
            androidBitmap.compress(compressFormat, if (format == ImageFormat.PNG) 100 else 92, out)
        }

        if (format == ImageFormat.JPEG) {
            // Ensure no EXIF metadata remains
            val exif = ExifInterface(file)
            exif.clearExif()
            exif.saveAttributes()
        }

        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    fun share(context: Context, uri: Uri, mime: String) {
        context.startActivity(Intent.createChooser(createShareIntent(uri, mime), null))
    }

    fun createShareIntent(uri: Uri, mime: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun ExifInterface.clearExif() {
    val tags = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_ARTIST
    )
    for (tag in tags) {
        setAttribute(tag, null)
    }
}

