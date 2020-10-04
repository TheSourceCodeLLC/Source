package net.sourcebot.module.moderation.command

import com.google.common.collect.Lists
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response

class HistoryCommand : ModerationCommand(
    "history", "Show punishment histories."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("target", "The member to view history of.", "self"),
        OptionalArgument("page", "The page of history to view.", 1)
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild)) ?: message.member!!
        val header = "${target.user.asTag}'s History"
        val history = punishmentHandler.getHistory(target)
        if (history.isEmpty()) return InfoResponse(
            header, "This user does not have any history."
        )
        val points = history.filter {
            it.points != null
        }.map { it.points!! }.sum()
        val pageNum = args.next(Adapter.int()) ?: 1
        val pages = Lists.partition(history, 5)
        val page = pages[pageNum - 1]
        return InfoResponse(
            header,
            """
                **Punishment Points:** $points
                
                **Incidents:**
                ${
                page.joinToString("\n") {
                    "**${it.id}:** ${it.type.name.toLowerCase().capitalize()}: _${it.reason}_"
                }
            }
            """.trimIndent()
        )
    }
}