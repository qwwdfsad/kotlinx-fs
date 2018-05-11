package kotlinx.fs.core.attributes

/**
 * Basic attributes associated with a file in a file system
 */
open class FileAttributes(
    val isDirectory: Boolean, val isFile: Boolean, val isSymbolicLink: Boolean,
    val creationTimeUs: Long, val lastAccessTimeUs: Long, val lastModifiedTimeUs: Long, val sizeBytes: Long
)

class PosixFileAttributes(
    isDirectory: Boolean, isFile: Boolean, isSymbolicLink: Boolean,
    creationTimeUs: Long, lastAccessTimeUs: Long, lastModifiedTimeUs: Long, sizeBytes: Long,
    val permissions: Set<PosixFilePermissions>
) :
    FileAttributes(
        isDirectory,
        isFile,
        isSymbolicLink,
        creationTimeUs,
        lastAccessTimeUs,
        lastModifiedTimeUs,
        sizeBytes
    )
