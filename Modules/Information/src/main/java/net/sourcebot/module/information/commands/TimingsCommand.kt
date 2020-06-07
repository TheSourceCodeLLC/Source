package net.sourcebot.module.information.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import java.time.Instant

class TimingsCommand : RootCommand() {
    override val name = "timings"
    override val description = "Show bot timings."
    override val aliases = arrayOf("ping", "latency")

    override fun execute(message: Message, args: Arguments): Alert {
        val sent = message.timeCreated.toInstant().toEpochMilli()
        val now = Instant.now().toEpochMilli()
        val difference = now - sent
        val gateway = message.jda.gatewayPing
        val rest = message.jda.restPing.complete()
        return InfoAlert(
            "Source Timings",
            "**Command Execution**: ${difference}ms\n" +
            "**Gateway Ping**: ${gateway}ms\n" +
            "**REST Ping**: ${rest}ms"
        )
    }
}