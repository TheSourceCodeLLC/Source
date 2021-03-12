package net.sourcebot.module.experience

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.experience.command.ExperienceLeaderboardCommand
import net.sourcebot.module.experience.data.ExperienceData
import net.sourcebot.module.experience.listener.ExperienceListener
import net.sourcebot.module.profiles.Profiles
import kotlin.math.pow

class Experience : SourceModule() {
    override fun onEnable() {
        subscribeEvents(ExperienceListener())
        registerCommands(ExperienceLeaderboardCommand())
    }

    companion object {
        @JvmStatic operator fun get(member: Member) =
            Profiles.proxyObject(member, "experience", ::ExperienceData)

        @JvmStatic fun getLevel(points: Long) =
            (points - 499.0).pow(1.0 / 3.0).toLong() + 1

        @JvmStatic fun totalPointsFor(level: Long) =
            499 + (level - 1.0).pow(3.0).toLong()
    }
}