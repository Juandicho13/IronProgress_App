package com.olarte.ironprogress.ui.settings

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.settings.ExperienceLevel
import com.olarte.ironprogress.domain.settings.TrainingGoal
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WeightUnit
import com.olarte.ironprogress.domain.settings.WorkoutAudio

/** Los pasos de la bienvenida, en orden. */
enum class OnboardingStep {
    WELCOME,
    GOAL,
    EXPERIENCE,
    DAYS,
    UNITS,
    DISCIPLINES,
    MODE,
    AUDIO,
    DONE,
    ;

    /** Los que se pueden omitir con "Omitir": toman el valor por defecto. */
    val optional: Boolean get() = this == MODE || this == AUDIO

    /** Las preguntas: tienen barra de progreso y botón Continuar. */
    val isQuestion: Boolean get() = this != WELCOME && this != DONE

    companion object {
        val questions: List<OnboardingStep> = entries.filter { it.isQuestion }
    }
}

/** Lo que se va respondiendo. null es "sin responder". */
data class OnboardingAnswers(
    val goal: TrainingGoal? = null,
    val experience: ExperienceLevel? = null,
    val daysPerWeek: Int = 3,
    val unit: WeightUnit? = null,
    val disciplines: Set<Discipline> = emptySet(),
    val proMode: Boolean? = null,
    val audio: WorkoutAudio? = null,
) {
    fun toProfile(): TrainingProfile = TrainingProfile(
        goal = goal ?: TrainingGoal.STRENGTH,
        experience = experience ?: ExperienceLevel.BEGINNER,
        unit = unit ?: WeightUnit.KG,
        daysPerWeek = daysPerWeek.coerceIn(TrainingProfile.DAYS_RANGE),
        disciplines = disciplines.ifEmpty { setOf(Discipline.STRENGTH) },
    )
}

/** A dónde va la app al terminar la bienvenida. */
enum class OnboardingExit {
    CREATE_ROUTINE,
    TODAY,
}

/** Lo que se guarda al terminar. Lo omitido queda con el valor por defecto de Ajustes. */
data class OnboardingResult(val profile: TrainingProfile, val proMode: Boolean, val audio: WorkoutAudio, val exit: OnboardingExit)

/**
 * La bienvenida paso a paso, sin pantalla: qué paso va, si se puede continuar y cómo se avanza,
 * se vuelve y se omite. La pantalla solo la dibuja; así la navegación se prueba sin interfaz.
 */
data class OnboardingFlow(val step: OnboardingStep = OnboardingStep.WELCOME, val answers: OnboardingAnswers = OnboardingAnswers()) {

    /** Continuar se apaga hasta que la pregunta tenga respuesta. */
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME, OnboardingStep.DONE, OnboardingStep.DAYS -> true
            OnboardingStep.GOAL -> answers.goal != null
            OnboardingStep.EXPERIENCE -> answers.experience != null
            OnboardingStep.UNITS -> answers.unit != null
            OnboardingStep.DISCIPLINES -> answers.disciplines.isNotEmpty()
            OnboardingStep.MODE -> answers.proMode != null
            OnboardingStep.AUDIO -> answers.audio != null
        }

    /** De 0 a 1 para la barra fina de arriba: la primera pregunta ya cuenta. */
    val progress: Float
        get() {
            val index = OnboardingStep.questions.indexOf(step)
            return if (index < 0) (if (step == OnboardingStep.DONE) 1f else 0f) else (index + 1).toFloat() / OnboardingStep.questions.size
        }

    fun next(): OnboardingFlow = if (!canContinue ||
        step == OnboardingStep.DONE
    ) {
        this
    } else {
        copy(step = OnboardingStep.entries[step.ordinal + 1])
    }

    fun back(): OnboardingFlow = if (step == OnboardingStep.WELCOME) this else copy(step = OnboardingStep.entries[step.ordinal - 1])

    /** Omitir deja el valor por defecto y pasa al siguiente paso. */
    fun skip(): OnboardingFlow {
        if (!step.optional) return this
        val filled = when (step) {
            OnboardingStep.MODE -> answers.copy(proMode = answers.proMode ?: false)
            OnboardingStep.AUDIO -> answers.copy(audio = answers.audio ?: WorkoutAudio.KEEP_VOLUME)
            else -> answers
        }
        return copy(answers = filled).next()
    }

    fun answer(transform: (OnboardingAnswers) -> OnboardingAnswers): OnboardingFlow = copy(answers = transform(answers))

    fun toggleDiscipline(discipline: Discipline): OnboardingFlow = answer {
        it.copy(disciplines = if (discipline in it.disciplines) it.disciplines - discipline else it.disciplines + discipline)
    }

    fun changeDays(delta: Int): OnboardingFlow = answer {
        it.copy(daysPerWeek = (it.daysPerWeek + delta).coerceIn(TrainingProfile.DAYS_RANGE))
    }

    fun result(exit: OnboardingExit): OnboardingResult = OnboardingResult(
        answers.toProfile(),
        answers.proMode ?: false,
        answers.audio ?: WorkoutAudio.KEEP_VOLUME,
        exit,
    )
}
