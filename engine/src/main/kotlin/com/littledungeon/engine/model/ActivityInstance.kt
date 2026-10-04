package com.littledungeon.engine.model

/** One concrete question the child sees. */
sealed interface ActivityInstance {
    val templateId: TemplateId
    val skill: SkillId
    val level: Int
    val scene: Scene
    val seed: Long
    val activityType: String
}

data class CountObjectsInstance(
    override val templateId: TemplateId,
    override val skill: SkillId,
    override val level: Int,
    override val scene: Scene,
    override val seed: Long,
    val answer: Int,
    val choices: List<Int>,
    val tapToCount: Boolean,
) : ActivityInstance {
    override val activityType: String get() = TYPE

    companion object {
        const val TYPE = "COUNT_OBJECTS"
    }
}
