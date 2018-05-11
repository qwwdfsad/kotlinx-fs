package kotlinx.fs.core

import kotlinx.fs.core.attributes.*
import kotlinx.fs.core.attributes.PosixFileAttributes
import kotlinx.fs.core.attributes.PosixFilePermissions
import java.nio.file.*
import java.nio.file.attribute.*
import java.util.concurrent.*
import java.util.stream.*
import kotlin.reflect.*
import java.nio.file.attribute.PosixFileAttributes as PosixAttributes
import java.nio.file.attribute.PosixFilePermission as PosixPermission

actual fun getDefaultFileSystem(): FileSystem = JvmFileSystem

object JvmFileSystem : FileSystem() {

    override fun getPath(first: String, vararg more: String) = java.nio.file.Paths.get(first, *more)!!

    override fun exists(path: Path): Boolean = Files.exists(path)

    @Suppress("UNCHECKED_CAST")
    override fun <T : FileAttributes> readAttributes(path: Path, attributesClass: KClass<T>): T {
        if (attributesClass == FileAttributes::class) {
            val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
            return FileAttributes(
                isDirectory = attributes.isDirectory,
                isFile = attributes.isRegularFile,
                isSymbolicLink = attributes.isSymbolicLink,
                creationTimeUs = attributes.creationTime().to(TimeUnit.MICROSECONDS),
                lastAccessTimeUs = attributes.lastAccessTime().to(TimeUnit.MICROSECONDS),
                lastModifiedTimeUs = attributes.lastModifiedTime().to(TimeUnit.MICROSECONDS),
                sizeBytes = attributes.size()
            ) as T
        }

        if (attributesClass == PosixFileAttributes::class) {
            val attributes = Files.readAttributes(path, PosixAttributes::class.java)
            return PosixFileAttributes(
                isDirectory = attributes.isDirectory,
                isFile = attributes.isRegularFile,
                isSymbolicLink = attributes.isSymbolicLink,
                creationTimeUs = attributes.creationTime().to(TimeUnit.MICROSECONDS),
                lastAccessTimeUs = attributes.lastAccessTime().to(TimeUnit.MICROSECONDS),
                lastModifiedTimeUs = attributes.lastModifiedTime().to(TimeUnit.MICROSECONDS),
                sizeBytes = attributes.size(),
                permissions = attributes.permissions().map { permissionsMapping[it]!! }.toSet()
            ) as T
        }

        throw UnsupportedOperationException("Unsupported attributes class $attributesClass")
    }

    override fun list(path: Path): List<Path> = Files.list(path).collect(Collectors.toList())

    override fun walkDirectory(path: Path, consumer: (Path) -> Unit) = Files.list(path).forEach(consumer)

    override fun createFile(path: Path): Path = Files.createFile(path)

    override fun createDirectory(path: Path): Path = Files.createDirectory(path)

    override fun createTemporaryDirectory(prefix: String): Path = Files.createTempDirectory(prefix)

    override fun createTemporaryFile(directory: Path, prefix: String, suffix: String): Path =
        Files.createTempFile(directory, prefix, suffix)

    override fun copy(source: Path, target: Path) = Files.copy(source, target)!!

    override fun move(source: Path, target: Path) = Files.move(source, target)!!

    override fun delete(path: Path): Boolean = Files.deleteIfExists(path)

    override fun newInputStream(path: Path): InputStream = Files.newInputStream(path)

    override fun newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)

    @JvmStatic
    private val permissionsMapping = mapOf<PosixPermission, PosixFilePermissions>(
        PosixPermission.OWNER_READ to PosixFilePermissions.OWNER_READ,
        PosixPermission.OWNER_WRITE to PosixFilePermissions.OWNER_WRITE,
        PosixPermission.OWNER_EXECUTE to PosixFilePermissions.OWNER_EXECUTE,


        PosixPermission.GROUP_READ to PosixFilePermissions.GROUP_READ,
        PosixPermission.GROUP_WRITE to PosixFilePermissions.GROUP_WRITE,
        PosixPermission.GROUP_EXECUTE to PosixFilePermissions.GROUP_EXECUTE,

        PosixPermission.OTHERS_READ to PosixFilePermissions.OTHERS_READ,
        PosixPermission.OTHERS_WRITE to PosixFilePermissions.OTHERS_WRITE,
        PosixPermission.OTHERS_EXECUTE to PosixFilePermissions.OTHERS_EXECUTE
    )
}
