package com.olarte.ironprogress.ui.voice

import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutAudioTest {

    private class Recorder : VoiceSpeaker {
        val said = mutableListOf<Pair<String, MusicBehavior>>()

        override fun speak(text: String, music: MusicBehavior) {
            said += text to music
        }

        override fun stop() = Unit

        override fun release() = Unit
    }

    @Test
    fun eachOptionMapsToWhatHappensWithTheMusic() {
        assertEquals(MusicBehavior.MIX, WorkoutAudio.KEEP_VOLUME.musicBehavior())
        assertEquals(MusicBehavior.DUCK, WorkoutAudio.DUCK.musicBehavior())
        assertEquals(MusicBehavior.PAUSE, WorkoutAudio.PAUSE_MUSIC.musicBehavior())
        assertNull(WorkoutAudio.NO_VOICE.musicBehavior())
    }

    @Test
    fun theSettingWinsOverWhatTheScreenAsks() {
        val recorder = Recorder()
        recorder.withAudio(WorkoutAudio.PAUSE_MUSIC).speak("Uno", MusicBehavior.DUCK)
        recorder.withAudio(WorkoutAudio.NO_VOICE).speak("Dos")
        assertEquals(listOf("Uno" to MusicBehavior.PAUSE), recorder.said)
    }

    @Test
    fun noVoiceTurnsTheVoiceOffAndTheOldSwitchCountsAsNoVoice() {
        val keep = AppSettings()
        assertEquals(WorkoutAudio.KEEP_VOLUME, keep.workoutAudio)

        val none = keep.withWorkoutAudio(WorkoutAudio.NO_VOICE)
        assertFalse(none.voice)
        assertEquals(WorkoutAudio.NO_VOICE, none.workoutAudio)

        val duck = none.withWorkoutAudio(WorkoutAudio.DUCK)
        assertTrue(duck.voice)
        assertEquals(WorkoutAudio.DUCK, duck.workoutAudio)

        assertEquals(WorkoutAudio.NO_VOICE, AppSettings(voice = false, audio = WorkoutAudio.DUCK).workoutAudio)
    }
}
