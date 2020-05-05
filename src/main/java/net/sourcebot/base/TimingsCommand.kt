package net.sourcebot.base

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.info.TimingsAlert
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.argument.Arguments
import java.time.Instant

class TimingsCommand : Command() {
    override val name = "timings"
    override val description = "Show timings such as Command Delay and REST / Gateway Latency."
    override val aliases = arrayOf("ping", "latency")

    override fun execute(message: Message, args: Arguments): Alert {
        val now = Instant.now().toEpochMilli()
        val created = message.timeCreated.toInstant().toEpochMilli()
        val delay = now - created
        val rest = message.jda.restPing.complete()
        val gateway = message.jda.gatewayPing
        return TimingsAlert(delay, rest, gateway)
    }
}
