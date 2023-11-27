package net.sourcebot.module.freegames.event

import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.freegames.data.Game

class FreeGameEvent(val guild: Guild, val games: List<Game>) : SourceEvent