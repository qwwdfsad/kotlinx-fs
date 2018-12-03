package kotlinx.fs.core.internal

import kotlinx.cinterop.*
import kotlinx.fs.core.*
import kotlinx.fs.core.internal.Posix.errno

// TODO native tests
class PosixFileOutputStream(private val fd: Int) : OutputStream() {

    private val buffer = byteArrayOf(0)

    override fun close(): Unit = Posix.close(fd)

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        if (offset < 0 || offset > buffer.size || count < 0 ||
            offset + count > buffer.size || offset + count < 0) {
            throw IndexOutOfBoundsException("Offset: $offset, count: $count")
        } else if (count == 0) {
            return
        }


        buffer.usePinned {
            if (platform.posix.write(fd, buffer.refTo(offset), count.toULong()) == -1L) {
                throw IOException("Failed to write to file with error code ${errno()}")
            }
        }
    }

    override fun write(oneByte: Int) {
        buffer[0] = oneByte.toByte()
        write(buffer, 0, 1)
    }

    override fun flush() {
        // nothing
    }
}
