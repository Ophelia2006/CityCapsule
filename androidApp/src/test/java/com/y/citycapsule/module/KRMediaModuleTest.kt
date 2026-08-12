package com.y.citycapsule.module

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class KRMediaModuleTest {
    @Test
    fun responseUsesTheSharedJsonCallbackContract() {
        val response = JSONObject(
            KRMediaModule.response(
                status = KRMediaModule.STATUS_SUCCESS,
                paths = listOf("file:///one.jpg", "file:///two.jpg")
            )
        )

        assertEquals("success", response.getString("status"))
        assertEquals("", response.getString("message"))
        assertEquals(2, response.getJSONArray("paths").length())
    }

    @Test
    fun managedStoreDeletesOnlyFilesInsideTheOriginalImageDirectory() {
        val filesDir = Files.createTempDirectory("citycapsule_media").toFile()
        val managedDirectory = filesDir.resolve("images/original").apply { mkdirs() }
        val managed = managedDirectory.resolve("owned image.jpg").apply { writeText("photo") }
        val outside = filesDir.resolve("outside.jpg").apply { writeText("keep") }
        val store = ManagedImageFileStore(filesDir)

        val rejected = store.delete(
            listOf(managed.toURI().toString(), outside.toURI().toString())
        )

        assertTrue(rejected.rejected)
        assertTrue(managed.exists())
        assertTrue(outside.exists())

        val deleted = store.delete(listOf(managed.toURI().toString()))

        assertFalse(deleted.rejected)
        assertEquals(listOf(managed.toURI().toString()), deleted.deletedPaths)
        assertFalse(managed.exists())
    }

    @Test
    fun managedStoreAlsoDeletesCameraTargetsInTheSameDirectory() {
        val filesDir = Files.createTempDirectory("citycapsule_camera").toFile()
        val cameraFile = filesDir.resolve("images/original/camera_1.jpg").apply {
            parentFile.mkdirs()
            writeText("photo")
        }

        val result = ManagedImageFileStore(filesDir).delete(listOf(cameraFile.toURI().toString()))

        assertFalse(result.rejected)
        assertFalse(cameraFile.exists())
    }

    @Test
    fun deletingOriginalAlsoDeletesItsDeterministicThumbnail() {
        val filesDir = Files.createTempDirectory("citycapsule_thumbnail_delete").toFile()
        val original = filesDir.resolve("images/original/capsule_1.jpg").apply { parentFile.mkdirs(); writeText("photo") }
        val thumbnail = filesDir.resolve("images/thumbnail/capsule_1.jpg.jpg").apply { parentFile.mkdirs(); writeText("thumb") }

        ManagedImageFileStore(filesDir).delete(listOf(original.toURI().toString()))

        assertFalse(original.exists())
        assertFalse(thumbnail.exists())
    }

    @Test
    fun cleanupHonoursReferencesAndGracePeriod() {
        val filesDir = Files.createTempDirectory("citycapsule_orphans").toFile()
        val originalDir = filesDir.resolve("images/original").apply { mkdirs() }
        val referenced = originalDir.resolve("referenced.jpg").apply { writeText("keep"); setLastModified(1) }
        val orphan = originalDir.resolve("orphan.jpg").apply { writeText("remove"); setLastModified(1) }
        val inFlight = originalDir.resolve("new.jpg").apply { writeText("grace") }

        val json = JSONObject(ManagedImageFileStore(filesDir).cleanupUnreferenced(
            setOf(referenced.toURI().toString()), 60 * 60 * 1000L
        ))

        assertEquals("success", json.getString("status"))
        assertTrue(referenced.exists())
        assertFalse(orphan.exists())
        assertTrue(inFlight.exists())
    }
}
