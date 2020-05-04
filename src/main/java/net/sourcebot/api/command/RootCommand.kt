package net.sourcebot.api.command

import net.sourcebot.api.module.SourceModule

abstract class RootCommand(val module: SourceModule) : Command()