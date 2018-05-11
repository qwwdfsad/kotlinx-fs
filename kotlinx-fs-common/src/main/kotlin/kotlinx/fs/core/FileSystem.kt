package kotlinx.fs.core

import kotlinx.fs.core.attributes.*
import kotlin.reflect.*

/**
 * Provides API to a file system and is the factory for objects to access files and its properties.
 * The main entry-point to filesystem API is  [Path] object, created by [getPath] factory method,
 * which provides access to all low-level file system primitives.
 * A file system implementation should be thread-safe.
 *
 * Process-level filesystem (which is usually system-level) is obtained by [getDefaultFileSystem] call.
 *
 * Example of usage:
 *
 * ```
 * val path = Files.getPath("file.txt") // <- relative path for working directory
 * path.create()
 * path.writeBytes(ByteArray(239) { it })
 * val bytes = path.readAllBytes() // <- reads all bytes
 * ```
 */
abstract class FileSystem {

    /**
     * Creates a [Path] from given sequence of strings, which are joined into single path in file-system specific way.
     * Parsing and conversion of [Path] is strictly filesystem-dependent [Path], created by one type of [FileSystem] cannot
     * be used by another and vice versa.
     */
    abstract fun getPath(first: String, vararg more: String): Path

    /**
     * Checks whether file by given path exists.
     * Doesn't throw, returns `false` either when the file doesn't exist or current implementation wasn't able to read it
     */
    abstract fun exists(path: Path): Boolean


    /**
     * Reads basic file attributes, available on all filesystems
     */
    fun readBasicAttributes(path: Path): FileAttributes = readAttributes(path)


    /**
     * Reads file attributes of a given class.
     * TODO discovery of available attribute classes at runtime
     *
     * @throws [UnsupportedOperationException] if given attributes class is not supported by file system
     * @throws [IOException] if I/O error occurred, e.g. file is unavailable to read
     */
    abstract fun <T : FileAttributes> readAttributes(path: Path, attributesClass: KClass<T>): T

    inline fun <reified T : FileAttributes> readAttributes(path: Path) = readAttributes(path, T::class)

    /**
     * Creates a new and empty file, failing if the file already exists
     * @throws IOException if file already exists or I/O error occurred
     */
    abstract fun createFile(path: Path): Path

    /**
     * Creates a new and directory, failing if the directory already exists
     * @throws IOException if directory already exists or I/O error occurred
     */
    abstract fun createDirectory(path: Path): Path

    /**
     * Creates a new directory in the default temporary-files directory, using the given prefix to generate its name
     * Temporary file storage is system and platform specific
     */
    abstract fun createTemporaryDirectory(prefix: String): Path

    /**
     * TODO
     */
    abstract fun createTemporaryFile(directory: Path, prefix: String = "", suffix: String = ""): Path

    /**
     * Copy a file to a target file.
     * If source is a directory, then content of a directory **is not copied**.
     * Implementation of this method is not atomic, so if an [IOException] is thrown during copy,
     * target may be left in inconsistent state.
     * File-specific attributes (such as permissions or access time) of source are not copied.
     * If source and target point to the same file, behaviour is platform-specific.
     *
     * @throws IOException if target already exists or I/O error occurred during copy
     * @return path to target file
     */
    abstract fun copy(source: Path, target: Path): Path

    /**
     * Move a file to a target file.
     * If source is a directory, then content of a directory is copied.
     * Implementation of this method is atomic.
     * File-specific attributes (such as permissions or access time) copy is implementation-specific detail
     * (TODO it's because it's not yet supported on API level)
     * If source and target point to the same file, behaviour is platform-specific.
     *
     * @throws IOException if target already exists or I/O error occurred during move
     * @return path to target file
     */
    abstract fun move(source: Path, target: Path): Path

    /**
     * Attempts to delete a file or a directory if it's empty.
     * Operation is not atomic because it's required to check whether given path is file or directory
     *
     * @throws IOException if directory is not empty or I/O error occurred during removal
     * @return `true` if the file existed and was deleted, `false` if the file didn't exist
     */
    abstract fun delete(path: Path): Boolean

    /**
     * Opens a file, returning an input stream to read from the file.
     * Resulting stream is not thread-safe.
     *
     * @throws IOException if an I/O error occurs or target file doesn't exist
     */
    abstract fun newInputStream(path: Path): InputStream

    /**
     * Opens or creates a file, returning an output stream that may be used to
     * write bytes to the file. Resulting stream is not thread-safe
     *
     * @throws IOException if an I/O error occurs
     */
    abstract fun newOutputStream(path: Path): OutputStream

    /**
     * Reads all entries in given directory.
     * Race-safety of this call is implementation-specific (TODO implement dirat in native and inspect node.js implementation)
     * in terms of concurrent modification of target directory
     * @throws IOException if I/O error occurred during iteration over directory
     */
    abstract fun list(path: Path): List<Path>

    /**
     * Iterates over entries in given directory in a lazy manner.
     * Race-safety of this call is implementation-specific (TODO implement dirat in native and inspect node.js implementation)
     * in terms of concurrent modification of target directory
     * TODO this API should be more granular.
     * E.g. way to stop iteration should be introduced
     * @throws IOException if I/O error occurred during iteration over directory
     */
    open fun walkDirectory(path: Path, consumer: (Path) -> Unit): Unit = list(path).forEach(consumer)


    /**
     * Reads all file content into single byte array
     * @throws IOException if I/O error occurred during read
     */
    open fun readBytes(path: Path): ByteArray =
    // TODO not very efficient
        newInputStream(path).use {
            val baos = ByteArrayOutputStream()
            var byte: Int = it.read()
            while (byte != -1) {
                baos.write(byte)
                byte = it.read()
            }
            baos.toByteArray()
        }

    /**
     * Writes content to the file, creating one if it doesn't exist.
     * If file exists, all its content is overwritten
     * @throws IOException if I/O error occurred during write
     */
    open fun writeBytes(path: Path, bytes: ByteArray): Unit =
        newOutputStream(path).use {
            it.write(bytes)
        }
}

expect fun getDefaultFileSystem(): FileSystem
