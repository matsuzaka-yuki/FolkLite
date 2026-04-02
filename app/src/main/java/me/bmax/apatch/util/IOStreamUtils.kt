package me.bmax.apatch.util

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import me.bmax.apatch.apApp
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream


private const val TAG = "IOStreamUtils"

val cr: ContentResolver get() = apApp.contentResolver

fun Uri.inputStream() = cr.openInputStream(this) ?: throw FileNotFoundException()

fun Uri.outputStream() = cr.openOutputStream(this, "rwt") ?: throw FileNotFoundException()

fun Uri.fileDescriptor(mode: String) =
    cr.openFileDescriptor(this, mode) ?: throw FileNotFoundException()

inline fun <In : InputStream, Out : OutputStream> withStreams(
    inStream: In,
    outStream: Out,
    withBoth: (In, Out) -> Unit
) {
    inStream.use { reader ->
        outStream.use { writer ->
            withBoth(reader, writer)
        }
    }
}

fun InputStream.copyAndClose(out: OutputStream) = withStreams(this, out) { i, o -> i.copyTo(o) }
fun InputStream.writeTo(file: File) = copyAndClose(file.outputStream())

fun InputStream.copyAndCloseOut(out: OutputStream) = out.use { copyTo(it) }

fun Uri.cacheToLocalFile(fileName: String? = null): File? {
    return try {
        val resolvedName = fileName ?: this.lastPathSegment?.substringAfterLast('/') ?: "module_${System.currentTimeMillis()}.zip"
        val destFile = File(apApp.cacheDir, "install_${resolvedName}")
        cr.openInputStream(this)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
            destFile
        } ?: run {
            Log.e(TAG, "Failed to open input stream for URI: $this")
            null
        }
    } catch (e: SecurityException) {
        Log.e(TAG, "Permission denied accessing URI: $this", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "Failed to cache URI to local file: $this", e)
        null
    }
}
