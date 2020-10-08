package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import java.time.Instant
import kotlin.math.abs

class TimingsCommand : RootCommand() {
    override val name = "timings"
    override val description = "Show bot timings."
    override val aliases = arrayOf("ping", "latency")
    override val permission = name

    override fun execute(message: Message, args: Arguments): Response {
        val sent = message.timeCreated.toInstant().toEpochMilli()
        val now = Instant.now().toEpochMilli()
        val difference = abs(sent - now)
        val gateway = message.jda.gatewayPing
        val rest = message.jda.restPing.complete()
        return TimingsResponse(difference, gateway, rest)
    }

    override fun postResponse(response: Response, forWhom: User, message: Message) {
        if (response !is TimingsResponse) return
        response.renderResponseTime(forWhom, message)
    }
}

class TimingsResponse(
    private val execution: Long,
    private val gateway: Long,
    private val rest: Long
) : StandardInfoResponse(
    "Source Timings",
    """
      **Command Execution**: ${execution}ms
      **Gateway Ping**: ${gateway}ms
      **REST Ping**: ${rest}ms
    """.trimIndent()
) {
    fun renderResponseTime(forWhom: User, message: Message) {
        val sent = message.timeCreated.toInstant().toEpochMilli()
        val now = Instant.now().toEpochMilli()
        val response = abs(sent - now)
        message.editMessage(
            StandardInfoResponse(
                "Source Timings",
                """
                    **Command Execution**: ${execution}ms
                    **Command Response**: ${response}ms
                    **Gateway Ping**: ${gateway}ms
                    **REST Ping**: ${rest}ms
                """.trimIndent()
            ).asEmbed(forWhom)
        ).queue()
    }
}