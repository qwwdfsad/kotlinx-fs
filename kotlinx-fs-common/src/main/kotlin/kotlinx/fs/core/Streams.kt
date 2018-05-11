package kotlinx.fs.core

expect open class IOException : Exception {
    constructor()
    constructor(message: String)
}

expect abstract class InputStream : Closeable {
    open fun available(): Int
    abstract fun read(): Int
    open fun read(b: ByteArray): Int
    open fun read(b: ByteArray, offset: Int, len: Int): Int
    open fun skip(n: Long): Long
}

expect class ByteArrayInputStream(buf: ByteArray) : InputStream {
    override fun read(): Int
}

expect abstract class OutputStream : Closeable {
    open fun flush()
    open fun write(buffer: ByteArray, offset: Int, count: Int)
    open fun write(buffer: ByteArray)
    abstract fun write(oneByte: Int)
}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // KT-17944
expect class ByteArrayOutputStream() : OutputStream {
    override fun write(oneByte: Int)
    fun toByteArray(): ByteArray
    fun size(): Int
}
