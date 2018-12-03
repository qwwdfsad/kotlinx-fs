package kotlinx.fs.core.internal

import kotlinx.cinterop.*
import platform.posix.*

object TemporaryDirectory {

    val temporaryDirectory: String = getenv("TMPDIR")!!.toKString()
    init {
        srand(clock().toUInt())
    }

    fun generateTemporaryDirectoryName(prefix: String): String {
        // TODO need something more robust
        val random = rand()
        return "$temporaryDirectory/$prefix-$random/"
    }
}
