package net.sourcebot.module.documentation.utility

import me.theforbiddenai.jenkinsparserkotlin.entities.Information
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.sourcebot.module.documentation.dochandlers.JenkinsHandler

/**
 * This class holds the data for the doc selectors
 *
 * @property docMessage The [Message] which contains the [MessageEmbed] for the doc selector
 * @property cmdMessage The [Message] which contains the command text
 * @property infoList The [List] of [Information] which are the available options to select
 * @property jenkinsHandler The [JenkinsHandler] to be able to convert information to a [DocResponse]
 */
data class SelectorModel(
    var docMessage: Message?,
    var cmdMessage: Message,
    val infoList: List<Information>,
    val jenkinsHandler: JenkinsHandler
) {

    /**
     * Makes sure the infoList isn't empty
     * @throws Exception if the info list is empty
     */
    init {
        if (infoList.isEmpty()) {
            throw Exception("Info list can not be empty!")
        }
    }

    /**
     * This is the companion object for the [SelectorCache] object
     */
    companion object {
        val selectorCache = SelectorCache()
    }

}