package kotlinx.fs.core.internal

import kotlinx.fs.core.internal.Posix.errno
import kotlinx.cinterop.*
import kotlinx.fs.core.*
import platform.posix.*
import kotlin.math.*

// TODO native test
class PosixFileInputStream(private val fd: Int) : InputStream() {

    private val buffer = byteArrayOf(0)

    override fun available(): Int {
        // TODO ?
        return 0 // No hints
    }

    override fun close(): Unit = Posix.close(fd)

    override fun read(): Int {
        if (read(buffer) != -1) {
            return buffer[0].toInt()
        }

        return -1
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, offset: Int, len: Int): Int {
        if (offset < 0 || offset > b.size || len < 0 ||
            offset + len > b.size || offset + len < 0
        ) {
            throw IndexOutOfBoundsException("Offset: $offset, length: $len")
        } else if (len == 0) {
            return 0
        }

        return b.usePinned<ByteArray, Int> {
            while (true) {
                val read = platform.posix.read(fd, it.addressOf(0), buffer.size.toULong()).toInt()
                if (read == 0) {
                    return -1 // Be compliant with API standard
                }

                if (read == -1) {
                    val errno = errno()
                    if (errno == EINTR) continue
                    throw IOException("Failed to read data with error code $errno")
                }

                return read
            }

            @Suppress("UNREACHABLE_CODE")
            throw AssertionError("Unreachable condition")
        }
    }

    override fun skip(n: Long): Long {
        if (n <= 0) {
            return 0
        }

        var remaining = n

        val size = remaining.coerceAtMost(1024).toInt()
        val skipBuffer = ByteArray(size)
        while (remaining > 0) {
            val read = read(skipBuffer, 0, min(size.toLong(), remaining).toInt())
            if (read < 0) {
                break
            }
            remaining -= read
        }

        return n - remaining
    }
}
