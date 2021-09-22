package net.sourcebot.module.moderation.command

import com.google.common.collect.Lists
import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.truncate
import net.sourcebot.api.wrapped
import net.sourcebot.api.zipAll
import net.sourcebot.module.moderation.Moderation

class HistoryCommand : ModerationRootCommand(
    "history", "Show punishment histories."
) {
    override val synopsis = Synopsis {
        optParam("target", "The user to view history of.", Adapter.single())
        optParam(
            "page", "The page of history to view.", Adapter.int(
                1, error = "The page to view must be at least 1!"
            ), 1
        )
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.optional<String, User>("target", sender.author) {
            SourceAdapter.user(sender.jda, it)
        }
        if (target.isBot) return StandardErrorResponse(
            "History Failure!", "Bots do not have history!"
        )
        val header = "${target.asTag}'s History"
        val punishmentHandler = Moderation.getPunishmentHandler(sender.guild)
        val historyList = punishmentHandler.getHistory(target.id)
        val reportList = punishmentHandler.getReportsAgainst(target)
        if (historyList.isEmpty() && reportList.isEmpty()) return StandardInfoResponse(
            header, "This user does not have any history."
        ).wrapped(target)
        val historyPages = Lists.partition(historyList, 5)
        val reportPages = Lists.partition(reportList, 5)
        val pages = historyPages.zipAll(reportPages)
        val pageNum = arguments.optional("page", 1)
        if (pageNum > pages.size) throw InvalidSyntaxException(
            "Page must be between 1 and ${pages.size}!"
        )
        val (history, reports) = pages[pageNum - 1]
        return StandardInfoResponse(
            header, "**Punishment Points:** ${punishmentHandler.getPoints(target)}"
        ).apply {
            if (history?.isNotEmpty() == true) {
                appendDescription(
                    """
                        
                        
                        **Incidents:**
                        ${
                        history.joinToString("\n") {
                            "**${it.id}:** ${it.heading}: _${it.reason.truncate(50)}_"
                        }
                    }
                    """.trimIndent()
                )
            }
            if (reports?.isNotEmpty() == true) {
                appendDescription(
                    """
                        
                        
                        **Reports:**
                        ${reports.joinToString("\n") { "**${it.id}**: _${it.reason.truncate(50)}_" }}
                    """.trimIndent()
                )
            }
            appendDescription("\n\nPage $pageNum / ${pages.size}")
        }.wrapped(target)
    }
}