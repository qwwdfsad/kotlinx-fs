package kotlinx.fs.core.internal

import kotlinx.cinterop.*
import kotlinx.fs.core.*
import kotlinx.fs.core.internal.Posix.errno
import kotlinx.io.core.*
import platform.posix.*

// TODO native test
@UseExperimental(ExperimentalIoApi::class)
class PosixFileInput(private val fd: Int) : AbstractInput() {
    override fun fill(): IoBuffer? {
        val byteArray = nativeHeap.allocArray<ByteVar>(512)

        val bytesRead = platform.posix.read(fd, byteArray, 512).toInt()
        if (bytesRead == 0) {
            return null
        } else if (bytesRead == -1) {
            val errno = errno()
//            if (errno == EINTR) continue
            throw IOException("Failed to read data with error code $errno")
        }

        return IoBuffer(byteArray, bytesRead)
    }

    override fun closeSource() {
        Posix.close(fd)
    }

}
