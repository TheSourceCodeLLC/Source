package net.sourcebot.module.profiles

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.profiles.data.ProfileManager

class Profiles : SourceModule() {
    override fun onEnable() {
        profileManager = ProfileManager(source.mongodb)
    }

    companion object {
        @JvmStatic
        lateinit var profileManager: ProfileManager
            internal set
    }
}