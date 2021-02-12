package net.sourcebot.module.experience

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.experience.data.ExperienceData
import net.sourcebot.module.experience.listener.ExperienceListener
import net.sourcebot.module.profiles.Profiles

class Experience : SourceModule() {
    override fun onEnable() {
        subscribeEvents(ExperienceListener())
    }

    companion object {
        @JvmStatic operator fun get(member: Member) =
            Profiles.proxyObject(member, "experience", ::ExperienceData)
    }
}