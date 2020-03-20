package util.zip

expect object ZipUtils {
    fun encode(byteArray: ByteArray): ByteArray
    fun decode(byteArray: ByteArray): ByteArray
}