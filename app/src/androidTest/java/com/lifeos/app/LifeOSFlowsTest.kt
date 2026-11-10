package com.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import java.io.File

@RunWith(AndroidJUnit4::class)
class LifeOSFlowsTest {
    @get:Rule
    val compose = createAndroidComposeRule<LifeOSTestActivity>()

    @Before
    fun resetLocalDatabase() {
        val dao = LifeOSDatabase.get(compose.activity).lifeOSDao()
        dao.clearTopics()
        dao.clearWorkoutLogs()
    }

    private fun showApp() {
        compose.setContent { LifeOS(compose.activity) }
        compose.waitForIdle()
    }

    @Test
    fun studyTopicsCanBeCompletedAndPersistedLocally() {
        showApp()
        compose.onNodeWithText("Study").performClick()
        compose.onNodeWithText("STUDY TRACKER").assertIsDisplayed()
        compose.onNodeWithText("DSA · Topic 1").assertIsDisplayed()
        compose.onNodeWithTag("topic-checkbox-1").performClick()
        compose.waitForIdle()

        val topic = LifeOSDatabase.get(compose.activity).lifeOSDao()
            .getTopics().first { it.id == 1L }
        assertEquals(true, topic.completed)
        assertEquals(170, LifeOSDatabase.get(compose.activity).lifeOSDao().topicCount())
    }

    @Test
    fun gymWorkoutCanBeLoggedAndStored() {
        showApp()
        compose.onNodeWithText("Gym").performClick()
        compose.onNodeWithText("Bench Press").performClick()
        compose.onNodeWithText("Log workout").assertIsDisplayed()

        compose.onNodeWithTag("workout-sets").performTextClearance()
        compose.onNodeWithTag("workout-sets").performTextInput("4")
        compose.onNodeWithTag("workout-reps").performTextClearance()
        compose.onNodeWithTag("workout-reps").performTextInput("8")
        compose.onNodeWithTag("workout-weight").performTextClearance()
        compose.onNodeWithTag("workout-weight").performTextInput("40")
        compose.onNodeWithText("Save set").performClick()
        compose.waitForIdle()

        val saved = LifeOSDatabase.get(compose.activity).lifeOSDao().getWorkoutLogs()
        assertEquals(1, saved.size)
        assertEquals("Bench Press", saved.first().exercise)
        assertEquals(4, saved.first().sets)
        assertEquals(8, saved.first().reps)
        assertEquals(40.0, saved.first().weight, 0.001)
    }

    @Test
    fun journalAndProgressScreensRender() {
        showApp()
        compose.onNodeWithText("Journal").performClick()
        compose.onNodeWithText("PHOTO JOURNAL").assertIsDisplayed()
        compose.onNodeWithText("Capture progress").assertIsDisplayed()

        compose.onNodeWithText("Progress").performClick()
        compose.onNodeWithText("YOUR PROGRESS").assertIsDisplayed()
        compose.onNodeWithText("Consistency over intensity.").assertIsDisplayed()
    }


    @Test
    fun localDatabaseIsEncryptedAtRest() {
        showApp()
        val dbFile = compose.activity.getDatabasePath("lifeos_private.db")
        assertEquals(true, dbFile.exists())
        val header = File(dbFile.absolutePath).inputStream().use { input ->
            ByteArray(16).also { input.read(it) }
        }
        val sqliteHeader = "SQLite format 3\\u0000".toByteArray(Charsets.US_ASCII)
        assertFalse(header.copyOfRange(0, sqliteHeader.size).contentEquals(sqliteHeader))
    }

    @Test
    fun pomodoroCanStartAndTick() {
        showApp()
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("POMODORO FOCUS"))
        compose.onNodeWithText("POMODORO FOCUS").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Start").performClick()
        compose.mainClock.advanceTimeBy(1_100L)
        compose.waitForIdle()
        compose.onNodeWithText("24:59").assertIsDisplayed()
    }
}
