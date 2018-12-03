package kotlinx.fs.core.internal

import kotlinx.fs.core.*
import platform.posix.*

internal object Posix {

    fun errno(): Int = posix_errno()

    fun close(fd: Int) {
        if (platform.posix.close(fd) == -1) {
            throw IOException("Failed to close file with error ${errno()}")
        }
    }
}
