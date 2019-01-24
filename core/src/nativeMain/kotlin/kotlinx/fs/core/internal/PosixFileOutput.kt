package kotlinx.fs.core.internal

import kotlinx.fs.core.*
import kotlinx.fs.core.internal.Posix.errno
import kotlinx.io.core.*

// TODO native tests
@UseExperimental(ExperimentalIoApi::class)
class PosixFileOutput(private val fd: Int) : AbstractOutput() {

    override fun close(): Unit = Posix.close(fd)

    @Suppress("CANNOT_OVERRIDE_INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    override fun last(buffer: IoBuffer) {
        buffer.writeDirect { ptr ->
            val result = platform.posix.write(fd, ptr, buffer.readRemaining.toULong()).toInt()
            if (result == -1) throw IOException("Failed to write to file with error code ${errno()}")
            result
        }
    }

    override fun flush() {
        last(IoBuffer.Empty)
    }
}
