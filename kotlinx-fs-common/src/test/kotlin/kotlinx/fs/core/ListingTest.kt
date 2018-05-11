package kotlinx.fs.core

import kotlin.test.*

class ListingTest : TestBase() {

    @Test
    fun testNonExistentListing() {
        val path = testDirectory("non-existent-listing")
        assertFailsWith<IOException> { path.list() }
    }

    @Test
    fun testEmptyListing() {
        val directory = testDirectory("empty-listing").createDirectory()
        assertTrue(directory.list().isEmpty())
    }

    @Test
    fun testListing() {
        val directory = testDirectory("listing").createDirectory()
        Paths.getPath(directory.toString(), "1.txt").createFile()
        Paths.getPath(directory.toString(), "2").createDirectory()

        val expected = listOf(testFile("listing/1"), testDirectory("listing/2"))
        assertEquals(expected.toSet(), directory.list().toSet())
    }

    @Test
    fun testNestedListing() {
        val directory = testDirectory("nested-listing").createDirectory()
        Paths.getPath(directory.toString(), "1.txt").createFile()
        val nested = Paths.getPath(directory.toString(), "2").createDirectory()
        Paths.getPath(nested.toString(), "3.txt").createFile()

        val expected = listOf(testFile("nested-listing/1"), testDirectory("nested-listing/2"))
        assertEquals(expected.toSet(), directory.list().toSet())
    }
}
