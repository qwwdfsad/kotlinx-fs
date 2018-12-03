package kotlinx.fs.core

import kotlinx.fs.core.Paths.fs
import kotlinx.fs.core.Paths.getPath
import kotlinx.fs.core.attributes.*

object Paths {

    fun getPath(first: String, vararg more: String): Path = fs().getPath(first, *more)
    fun getPath(first: Path, vararg more: String): Path = fs().getPath(first.toString(), *more)
    fun getPath(first: Path, vararg more: Path): Path =
        fs().getPath(first.toString(), *more.map { it.toString() }.toTypedArray())

    fun createTemporaryDirectory(prefix: String): Path = fs().createTemporaryDirectory(prefix)

    fun fs(): FileSystem = getDefaultFileSystem()
}

fun Path(first: String, vararg more: String): Path = getPath(first, *more)
fun Path(first: Path, vararg more: String): Path = getPath(first, *more)
fun Path(first: Path, vararg more: Path): Path = getPath(first, *more)

/*
 * Integration point with external filesystems here:
 * Add FsAwarePath interface, add check to fs() and return path's fs if it's present and make
 * all calls on it.
 *
 * E.g.
 * val path = zipFs.getPath("archive.zip")
 * val files = path.list() // <- list is called on zip fs
 * val content = path.readAllBytes() <- decompressed content
 *
 * Impl note: don't forget to add defensive type checks with human-readable errors in default FS implementations
 */

/**
 * Concatenates current path with given string ignoring its trailing and leading path separators.
 *
 * ```
 * val base = Path("/base")
 * (base + "foo.txt").toString() == (base + "///foo.txt///") // true
 * ```
 */
operator fun Path.plus(other: String): Path = getPath(this, other)

/**
 * Concatenates current path with given path ignoring its trailing and leading path separators.
 * This **does not** resolves [other] against current and ignores whether [other] is relative or absolute.
 * This operator also ignores type of [other] and returns [Path] bound to the same file system as [this] // TODO this is debatable
 *
 * ```
 * val base = Path("/base")
 * val relative = Path("foo")
 * val absolute = Path("/foo")
 *
 * (base + relative).toString() == (base + absolute) // true
 * ```
 */
operator fun Path.plus(other: Path): Path = getPath(this, other)

// TODO document it even though it's all delegates to FS
fun Path.createFile(): Path = fs().createFile(this)

fun Path.exists(): Boolean = fs().exists(this)

fun Path.createDirectory(): Path = fs().createDirectory(this)

fun Path.createTemporaryFile(prefix: String = "", suffix: String = ""): Path =
    fs().createTemporaryFile(this, prefix, suffix)

/**
 * Checks whether the file is a regular file.
 * Returns `false` if the file does't exist, is not a file, or it cannot be determined if the file is a regular file or not.
 */
fun Path.isRegularFile(): Boolean {
    return try {
        fs().readBasicAttributes(this).isFile
    } catch (e: IOException) {
        return false
    }
}

/**
 * Checks whether the file is a directory.
 * Returns `false` if the directory doesn't exist, is not a file, or it cannot be determined if the file is a regular file or not.
 */
fun Path.isDirectory(): Boolean {
    return try {
        fs().readBasicAttributes(this).isDirectory
    } catch (e: IOException) {
        return false
    }
}

/**
 * Returns the size of the specified file in bytes.
 * If given file is a directory, then its size is calculated recursively.
 * If a directory or subdirectory is security restricted, its size will not be included.
 * TODO symlinks
 */
fun Path.totalSize(): Long {
    // TODO not very efficient
    var result = 0L

    if (isDirectory()) {
        try {
            walkDirectory { result += it.totalSize() }
        } catch (e: IOException) {
            // Ignored, no access
        }
    } else {
        result += attributesOrNull()?.sizeBytes ?: 0L
    }

    return result
}

private fun Path.attributesOrNull(): FileAttributes? = try {
    attributes()
} catch (e: IOException) {
    null
}

fun Path.attributes(): FileAttributes = fs().readAttributes(this)

inline fun <reified T : FileAttributes> Path.attributesOfType(): T = fs().readAttributes(this)

fun Path.list(): List<Path> = fs().list(this)

fun Path.walkDirectory(consumer: (Path) -> Unit): Unit = fs().walkDirectory(this, consumer)

fun Path.copyTo(target: Path): Path = fs().copy(this, target)

fun Path.moveTo(target: Path): Path = fs().move(this, target)

fun Path.deleteIfExists(): Boolean = fs().delete(this)

fun Path.delete() {
    if (!fs().delete(this)) {
        throw IOException("Path $this doesn't exist")
    }
}

fun Path.deleteDirectory() {
    // TODO doesn't work on Windows (at all I guess) and is not very efficient
    fs().list(this).forEach {
        if (it.isDirectory()) {
            it.deleteDirectory()
        } else {
            it.delete()
        }

    }

    this.delete()
}

fun Path.newInputStream(): InputStream = fs().newInputStream(this)

fun Path.readAllBytes(): ByteArray = fs().readBytes(this)

fun Path.newOutputStream(): OutputStream = fs().newOutputStream(this)

fun Path.writeBytes(content: ByteArray): Unit = fs().writeBytes(this, content)
