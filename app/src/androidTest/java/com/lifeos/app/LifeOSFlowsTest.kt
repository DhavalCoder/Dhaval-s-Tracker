package com.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

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
        compose.onNodeWithText("DSA · Topic 1").performClick()
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

        compose.onNodeWithText("Sets").performTextClearance()
        compose.onNodeWithText("Sets").performTextInput("4")
        compose.onNodeWithText("Reps per set").performTextClearance()
        compose.onNodeWithText("Reps per set").performTextInput("8")
        compose.onNodeWithText("Weight (kg)").performTextClearance()
        compose.onNodeWithText("Weight (kg)").performTextInput("40")
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
    fun pomodoroCanStartAndTick() {
        showApp()
        compose.onNodeWithText("POMODORO FOCUS").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Start").performClick()
        compose.mainClock.advanceTimeBy(1_100L)
        compose.waitForIdle()
        compose.onNodeWithText("24:59").assertIsDisplayed()
    }
}
