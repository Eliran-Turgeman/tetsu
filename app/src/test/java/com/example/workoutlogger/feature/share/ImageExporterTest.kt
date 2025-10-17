package com.example.workoutlogger.feature.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.workoutlogger.feature.share.data.ImageFormat
import com.example.workoutlogger.feature.share.export.ImageExporter
import androidx.compose.ui.graphics.asImageBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageExporterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `export clears jpeg exif`() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).asImageBitmap()
        val file = ImageExporter.exportToCacheFile(context, bitmap, ImageFormat.JPEG)
        val exif = androidx.exifinterface.media.ExifInterface(file)
        assertNull(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
        assertNull(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
    }

    @Test
    fun `share intent contains stream`() {
        val uri = Uri.parse("content://com.example.workoutlogger/exports/file.png")
        val intent = ImageExporter.createShareIntent(uri, "image/png")
        assertEquals("image/png", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }
}

