package kotlinx.fs.core

import kotlinx.fs.core.attributes.*
import kotlinx.fs.core.internal.*
import kotlin.reflect.*

actual fun getDefaultFileSystem(): FileSystem = JsFileSystem

object JsFileSystem : FileSystem() {

    private val fs: dynamic = require("fs")
    private val tmpDir = require("os").tmpdir() as String

    override fun getPath(first: String, vararg more: String): Path {
        if (more.isEmpty()) {
            return UnixPath(first)
        }
        return UnixPath("$first/" + more.joinToString("/"))
    }

    override fun exists(path: Path): Boolean = fs.existsSync(path.str()) as Boolean

    override fun createFile(path: Path): Path {
        fs.openSync(path.str(), "w")
        return path
    }

    override fun createDirectory(path: Path): Path {
        try {
            fs.mkdirSync(path.str())
        } catch (e: dynamic) {
            throw IOException("Failed to create directory: $e")
        }
        return path
    }

    override fun createTemporaryDirectory(prefix: String): Path {
        val absolutePath = fs.mkdtempSync("$tmpDir/$prefix") as String
        return UnixPath(absolutePath)
    }

    override fun createTemporaryFile(directory: Path, prefix: String, suffix: String): Path = TODO("Not implemented")

    override fun copy(source: Path, target: Path): Path {
        try {
            // COPYFILE_EXCL to fail if target exists
            fs.copyFileSync(source.str(), target.str(), fs.constants.COPYFILE_EXCL)
        } catch (e: dynamic) {
            throw IOException("Failed to copy $source to $target: $e")
        }

        return target
    }

    override fun move(source: Path, target: Path): Path {
        if (target.exists()) {
            throw IOException("File $target already exists")
        }

        try {
            // COPYFILE_EXCL to fail if target exists
            fs.renameSync(source.str(), target.str())
        } catch (e: dynamic) {
            throw IOException("Failed to move $source to $target: $e")
        }

        return target
    }

    override fun delete(path: Path): Boolean {
        return try {
            if (path.isDirectory()) {
                fs.rmdirSync(path.str())
            } else {
                fs.unlinkSync(path.str())
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    override fun newInputStream(path: Path): InputStream {
        // TODO Doesn't work with large files, can be buffered
        try {
            val buffer = fs.readFileSync(path.str())
            val array = js("Array").prototype.slice.call(buffer, 0).unsafeCast<ByteArray>()
            return ByteArrayInputStream(array)
        } catch (e: dynamic) {
            throw IOException("Failed to create an input stream for $path: $e")
        }
    }

    override fun newOutputStream(path: Path): OutputStream {
        if (path.isDirectory()) throw IOException("Cannot create output stream for directory")

        try {
            // TODO this is really stupid
            return object : ByteArrayOutputStream() {
                override fun close() {
                    val content = toByteArray()
                    fs.writeFileSync(path.str(), js("Buffer").from(content))
                }

            }
        } catch (e: dynamic) {
            throw IOException("Failed to create an output stream for $path: $e")
        }
    }

    override fun list(path: Path): List<Path> =
        try {
            (fs.readdirSync(path.str()) as Array<String>).map { UnixPath(path.str() + "/" + it) }.toList()
        } catch (e: Throwable) {
            throw IOException("Failed to read listing for $path: $e")
        }


    override fun <T : FileAttributes> readAttributes(path: Path, attributesClass: KClass<T>): T {
        if (attributesClass != PosixFileAttributes::class && attributesClass != FileAttributes::class) {
            throw UnsupportedOperationException("Unsupported attributes class $attributesClass")
        }

        @Suppress("UNCHECKED_CAST")
        return readAttributesImpl(path) as T? ?: throw IOException("No such file: $path")
    }

    @Suppress("UnsafeCastFromDynamic")
    private fun readAttributesImpl(path: Path): FileAttributes? {
        return try {
            val attributes = fs.lstatSync(path.str())

            PosixFileAttributes(
                isDirectory = attributes.isDirectory() as Boolean,
                isFile = attributes.isFile() as Boolean,
                isSymbolicLink = attributes.isSymbolicLink() as Boolean,
                creationTimeUs = (attributes.ctimeMs as Double).toLong() * 1000,
                lastAccessTimeUs = (attributes.atimeMs as Double).toLong() * 1000,
                lastModifiedTimeUs = (attributes.mtimeMs as Double).toLong() * 1000,
                sizeBytes = (attributes.size as Double).toLong(),
                permissions = parsePermissions((attributes.mode as Double).toInt()))
        } catch (e: dynamic) {
            null
        }
    }

    private fun parsePermissions(mode: Int): Set<PosixFilePermissions> {
        val result = mutableSetOf<PosixFilePermissions>()
        if (mode and 256 != 0) result.add(PosixFilePermissions.OWNER_READ)
        if (mode and 128 != 0) result.add(PosixFilePermissions.OWNER_WRITE)
        if (mode and 64 != 0) result.add(PosixFilePermissions.OWNER_EXECUTE)

        if (mode and 32 != 0) result.add(PosixFilePermissions.GROUP_READ)
        if (mode and 16 != 0) result.add(PosixFilePermissions.GROUP_WRITE)
        if (mode and 8 != 0) result.add(PosixFilePermissions.GROUP_EXECUTE)

        if (mode and 4 != 0) result.add(PosixFilePermissions.OTHERS_READ)
        if (mode and 2 != 0) result.add(PosixFilePermissions.OTHERS_WRITE)
        if (mode and 1 != 0) result.add(PosixFilePermissions.OTHERS_EXECUTE)
        return result
    }
}

private fun Path.str(): String = (this as UnixPath).normalizedPath

private external fun require(module: String): dynamic
