# kotlinx-fs [WIP]
Cross-platform file API in pure Kotlin which provides uniform path-based interface for JVM, JS and Native.


## Quick example
```kotlin
object Ls : FsOperation {

    /*
     * Expected output:
     * 
     *  rw-r--r--	1.5K	1526040936000000	foo.txt
     *  rw-r--r--	5.2M	1526040918000000	bar.txt
     *  drwxr-xr-x     0    1524819687978690    dir
     */
    override fun execute(path: String) {
        val path = Paths.getPath(path) // Get Path using default file system
        if (!path.exists()) {
            println("ls: $path: No such file or directory")
        }

        if (path.isDirectory()) {
            path.walkDirectory { println(describe(it)) }
        } else {
            println(describe(path))
        }

    }

    private fun describe(path: Path): String {
        val attributes = path.attributesOfType<PosixFileAttributes>()
        return attributes.permissions.toHumanReadableString() + "\t" + path.totalSize().prettify() +
                "\t" + attributes.lastAccessTimeUs + "\t" + path.fileName
    }
}
```

This is a simplified `ls` implementation, which works on JS, JVM and Native 

## API description
The main API element is `Path` interface.
`Path` represents a path in the file system, which can point to file, directory or symbolic link.
Path can be absolute or relative.
`Path` instance can be obtained directly from filesystem (`FileSystem.getPath(str)`) or via `Paths.getPath(string)` call.
In the latter default file system will be used.


Internally path is represented as a sequence of elements, which elements may be extracted directly from `Path` using its own API:

```kotlin
val path = Paths.get("/Users/foo/Desktop/bar.txt")
println(path.getFileName()) // Prints bar.txt
println(path.isAbsolute())  // Prints true
println(path.getParent())   // Prints /Users/foo/Desktop/
println(path.getName(1))    // Prints foo
```


`Path` extensions from `kotlinx.fs.core.Path` are primary endpoint for reading, writing and manipulating files and directories.
All these extensions points call underlying filesystem and may throw `IOException` if any file system error is encountered.
```kotlin
val file = Paths.get("/Users/foo/Desktop/bar.txt")
println(file.isDirectory()) // false
println(format(file.readBytes())) // Will print file's content

val child = Paths.get(file, "nested.txt")
child.createFile() // Will throw IOException
```



The file system is a pluggable mechanism. Once obtained, `Path` can be used without direct access to file system, but all I/O
operations will be performed by the file system, responsible for a created path.   


## Platform support
`kotlinx-fs` supports JVM, Node.js and POSIX-compliant OSes (Mac OS X and Linux).
Native Windows is not supported.

On JVM unspecified properties (such as following symbolic links) of `FileSystem` are inherited from standard JVM file system (see `java.nio.file.FileSystems`).
On JS unspecified properties are inherited from `fs`.



## TODO list
* Integrate with `kotlinx-io` to provide decent read/write API
* Basic interface for permissions and platform-dependent attributes modification
* Symlinks support
* Random access files
* Path builders (`+`, relative builders etc.)
* More specific exceptions
* Relative path operations
* Common utility functions (based on `stdlib` and `Apache commons-io`)
* Cross-FS interaction (e.g. copying from ZipFS to current FS)


