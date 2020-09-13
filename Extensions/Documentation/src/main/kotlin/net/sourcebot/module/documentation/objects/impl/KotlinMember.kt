package net.sourcebot.module.documentation.objects.impl

import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.capitalizeAllWords
import org.jsoup.nodes.Element

class KotlinMember(
    private val memberElement: Element,
    val typeName: String
) : KotlinInformation() {

    override lateinit var url: String
    override lateinit var name: String
    override val type: String by lazy { retrieveType() }
    override val description: String by lazy { retrieveDescription(memberElement) }
    override val tags: ArrayList<String> by lazy { retrieveTags(memberElement) }

    init {
        val header = memberElement.selectFirst("h4")
        val anchor = header.selectFirst("a")
        val documentUri = memberElement.ownerDocument().baseUri()

        name = header.text()
        url = documentUri + anchor.attr("href")
    }

    override fun createResponse(): DocResponse {
        val hyperlink = MarkdownUtil.maskedLink("$typeName#$name", url)
        val docResponse = DocResponse()

        docResponse.setAuthor("Kotlin Documentation", null, iconUrl)
        docResponse.setDescription("**__${hyperlink}__**\n$description")

        return docResponse
    }

    private fun retrieveType(): String {
        val parentDiv = memberElement.parent()
        val headerName = parentDiv.previousElementSibling().text()
        val pluralType = headerName.capitalizeAllWords()

        return if (pluralType.endsWith("ies")) {
            pluralType.replace("ies", "y")
        } else {
            pluralType.removeSuffix("s")
        }
    }

}