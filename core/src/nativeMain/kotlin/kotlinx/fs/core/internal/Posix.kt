package kotlinx.fs.core.internal

import platform.posix.*

internal object Posix {
    fun errno(): Int = posix_errno()
}
