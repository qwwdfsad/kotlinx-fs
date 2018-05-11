package kotlinx.fs.core

import kotlinx.cinterop.*
import kotlinx.fs.core.attributes.*
import kotlinx.fs.core.internal.*
import kotlinx.fs.core.internal.Posix.errno
import kotlinx.fs.core.internal.TemporaryDirectory.generateTemporaryDirectoryName
import platform.posix.*
import kotlin.reflect.*


actual fun getDefaultFileSystem(): FileSystem = PosixFileSystem

object PosixFileSystem : FileSystem() {

    override fun getPath(first: String, vararg more: String): Path {
        if (more.isEmpty()) {
            return UnixPath(first)
        }
        return UnixPath("$first/" + more.joinToString("/"))
    }

    override fun exists(path: Path): Boolean = access(path.str(), F_OK) == 0

    override fun <T: FileAttributes> readAttributes(path: Path, attributesClass: KClass<T>): T {
        if (attributesClass != FileAttributes::class && attributesClass != PosixFileAttributes::class) {
            throw UnsupportedOperationException("File attributes of class $attributesClass are not supported by PosixFileSystem")
        }

        val stat = nativeHeap.alloc<stat>()
        if (lstat(path.str(), stat.ptr) == -1) {
            throw IOException("Failed to call 'lstat' on file $path with error code ${errno()}")
        }

        val fileType = stat.st_mode.toInt() and S_IFMT
        val permissions = parsePermissions(stat.st_mode.toInt())

        @Suppress("UNCHECKED_CAST")
        return PosixFileAttributes(
            isDirectory = fileType == S_IFDIR,
            isFile = fileType == S_IFREG,
            isSymbolicLink = fileType == S_IFLNK,
            creationTimeUs = stat.st_ctimespec.micros(),
            lastAccessTimeUs = stat.st_atimespec.micros(),
            lastModifiedTimeUs = stat.st_mtimespec.micros(),
            sizeBytes = stat.st_size,
            permissions = permissions
        ) as T
    }

    private fun parsePermissions(mode: Int): Set<PosixFilePermissions> {
        val result = mutableSetOf<PosixFilePermissions>()
        if (mode and S_IRUSR != 0) result.add(PosixFilePermissions.OWNER_READ)
        if (mode and S_IWUSR != 0) result.add(PosixFilePermissions.OWNER_WRITE)
        if (mode and S_IXUSR != 0) result.add(PosixFilePermissions.OWNER_EXECUTE)

        if (mode and S_IRGRP != 0) result.add(PosixFilePermissions.GROUP_READ)
        if (mode and S_IWGRP != 0) result.add(PosixFilePermissions.GROUP_WRITE)
        if (mode and S_IXGRP != 0) result.add(PosixFilePermissions.GROUP_EXECUTE)

        if (mode and S_IROTH != 0) result.add(PosixFilePermissions.OTHERS_READ)
        if (mode and S_IWOTH != 0) result.add(PosixFilePermissions.OTHERS_WRITE)
        if (mode and S_IXOTH != 0) result.add(PosixFilePermissions.OTHERS_EXECUTE)

        if (mode and S_ISUID != 0) result.add(PosixFilePermissions.SETUID)
        if (mode and S_ISGID != 0) result.add(PosixFilePermissions.SETGID)
        if (mode and S_ISVTX != 0) result.add(PosixFilePermissions.STICKY_BIT)

        return result
    }

    override fun createFile(path: Path): Path {
        // 0x1B6 hex == 438 == 0666 oct
        open(path.str(), O_WRONLY or O_CREAT, 0x1B6)
        return path
    }

    override fun createDirectory(path: Path): Path {
        // 0x1FF hex == 511 == 0777 oct
        if (mkdir(path.str(), 0x1FF) == -1) {
            throw IOException("Failed to create directory ${path.str()} with error code {${errno()}}")
        }
        return path
    }

    override fun createTemporaryDirectory(prefix: String): Path {
        val path = UnixPath(generateTemporaryDirectoryName(prefix))
        createDirectory(path)
        return path
    }

    override fun createTemporaryFile(directory: Path, prefix: String, suffix: String): Path {
        TODO("not implemented")
    }

    override fun copy(source: Path, target: Path): Path {
        if (exists(target)) {
            throw IOException("Path $target already exists")
        }

        val attributes = readBasicAttributes(source)
        when {
            attributes.isDirectory -> {
                createDirectory(target)

            }
            attributes.isFile -> {
                copyFile(source, target)

            }
            else -> throw IOException("Links are not supported by implementation")
        }

       return target
    }

    override fun move(source: Path, target: Path): Path {
        if (exists(target)) {
            throw IOException("File $target already exists")
        }

        if (rename(source.str(), target.str()) == -1) {
            throw IOException("Failed to move $source to $target with error code ${errno()}")
        }

        return target
    }

    private fun copyFile(source: Path, target: Path) {
        // TODO can be implemented in raw POSIX more efficiently
        val buffer = ByteArray(512)
        newOutputStream(target).use { output ->
            newInputStream(source).use { input ->
                var read: Int
                do {
                    read = input.read(buffer)
                    if (read != -1)  output.write(buffer, 0, read)
                } while (read != -1)
            }
        }
    }

    override fun delete(path: Path): Boolean {
        val isDirectory = path.isDirectory()
        val hasError = if (isDirectory) {
            rmdir(path.str()) == -1
        } else {
            unlink(path.str()) == -1
        }

        val error = if (hasError) errno() else 0

        if (error != 0 && error != ENOENT) {
            throw IOException("Failed to delete ${path.str()} (isDirectory = $isDirectory) with error code $error")
        }

        return error != ENOENT
    }

    override fun newInputStream(path: Path): InputStream {
        val fd = open(path.str(), O_RDONLY)
        if (fd == -1) {
            throw IOException("Failed to open ${path.str()} for reading with error code ${errno()}")
        }

        return PosixFileInputStream(fd)
    }

    override fun newOutputStream(path: Path): OutputStream {
        val fd = open(path.str(), O_CREAT or O_WRONLY or O_TRUNC, 0x1B6) // TODO constant
        if (fd == -1) {
            throw IOException("Failed to open ${path.str()} for writing with error code ${errno()}")
        }

        return PosixFileOutputStream(fd)
    }

    // TODO say how unsafe it is without openat
    override fun list(path: Path): List<Path> {
        val result = mutableListOf<Path>()
        walkDirectory(path) { result.add(it) }
        return result
    }

    override fun walkDirectory(path: Path, consumer: (Path) -> Unit) {
        val dirPtr = opendir(path.str())
                ?: throw IOException("Failed to open directory $path with error code ${errno()}")

        try {
            var dirStruct = readdir(dirPtr)
            while (dirStruct != null) {
                val name = dirStruct.pointed.d_name.toKString()
                if (name != "." && name != "..") {
                    consumer(UnixPath("$path/$name"))
                }

                dirStruct = readdir(dirPtr)
            }

            // TODO Can't check for errors here because there is no way to reset errno
//            if (errno() != 0) {
//                throw IOException("Failed to read from dir $path with error code ${errno()}")
//            }
        } finally {
            // TODO we need some supression/cause mechanism here
            if (closedir(dirPtr) == -1) {
                throw IOException("Failed to close directory $path with error code ${errno()}")
            }
        }
    }
}

@PublishedApi
internal fun Path.str(): String = (this as UnixPath).normalizedPath
@PublishedApi
internal fun timespec.micros(): Long = tv_sec * 1000000L + tv_nsec / 1000L
