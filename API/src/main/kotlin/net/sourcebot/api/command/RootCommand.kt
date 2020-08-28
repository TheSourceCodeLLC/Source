package net.sourcebot.api.command

import net.sourcebot.api.module.SourceModule

abstract class RootCommand : Command() {
    internal lateinit var module: SourceModule
}