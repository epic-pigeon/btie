package util.zip

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual object ZipUtils {
    actual fun encode(byteArray: ByteArray): ByteArray {
        println(byteArray.size)
        val byteOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteOutputStream as OutputStream?).use { it.write(byteArray) }
        return byteOutputStream.toByteArray()
    }

    actual fun decode(byteArray: ByteArray): ByteArray =
        GZIPInputStream(byteArray.inputStream()).readBytes()
}