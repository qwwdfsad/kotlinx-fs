package kotlinx.fs.core

import kotlinx.fs.core.Paths.createTemporaryDirectory
import kotlinx.fs.core.Paths.getPath
import kotlin.test.*

open class TestBase {

    private val testFolder: Path = createTemporaryDirectory("kotlinx-fs-common")

    @AfterTest
    fun tearDown() {
        testFolder.deleteDirectory()
    }

    fun testFile(path: String): Path = getPath(testFolder.toString(), "$path.txt")
    fun testDirectory(path: String): Path = getPath(testFolder.toString(), path)
}
