package net.sourcebot.module.documentation.objects.impl

import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.appendIfRoom
import net.sourcebot.module.documentation.utility.toMarkdown
import net.sourcebot.module.documentation.utility.truncate
import org.jsoup.nodes.Document

class KotlinType(
    private val document: Document,
) : KotlinInformation() {

    override val url: String = document.baseUri()
    override var name: String = document.selectFirst("h1").text()
    override lateinit var type: String
    override var description: String = ""
    override var tags: ArrayList<String> = arrayListOf()

    private val memberMap: ArrayList<KotlinMember> = retrieveMembers()

    init {
        val mainPageDiv = document.selectFirst("div.node-page-main")
        name = document.selectFirst("h1").text()
        description = retrieveDescription(mainPageDiv)
        tags = retrieveTags(mainPageDiv)
        initializeType()

    }

    fun searchMembers(query: String): ArrayList<KotlinMember> {
        val modifiedQuery = query.replace("#", ".")
        val foundMembersArray: ArrayList<KotlinMember> = arrayListOf()

        memberMap.filter { it.name.equals(modifiedQuery, true) }
            .forEach { foundMembersArray.add(it) }

        return foundMembersArray
    }

    override fun createResponse(): DocResponse {
        val stringLengthLimit = 300
        val paramHeader = document.selectFirst("h3#parameters")

        val hyperlink = MarkdownUtil.maskedLink(name, url)
        val docResponse = DocResponse()

        docResponse.setAuthor("Kotlin Documentation", null, iconUrl)
        docResponse.setDescription("**__${hyperlink}__**\n$description")

        if (paramHeader != null) {
            val paramParagraphElement = paramHeader.nextElementSibling()
            val paramDesc = hyperlinksToMarkdown(paramParagraphElement).toMarkdown().truncate(stringLengthLimit)
            docResponse.addField("Parameters:", paramDesc, false)
        }

        retrieveMemberStringBuilders(stringLengthLimit)
            .forEach { (fieldName, fieldDescription) ->
                if (fieldDescription.isBlank()) return@forEach
                docResponse.addField(fieldName, fieldDescription, false)
            }

        return docResponse
    }

    private fun retrieveMemberStringBuilders(limit: Int): Map<String, String> {
        val memberSBMap: MutableMap<String, String> = mutableMapOf()

        val propertiesSB = StringBuilder()
        val functionSB = StringBuilder()
        val extPropSB = StringBuilder()
        val extFuncSB = StringBuilder()
        val inheritorsSB = StringBuilder()

        memberMap.forEach {
            val type = it.type.toLowerCase()
            val appendStr = "`${it.name}` "
            when (type) {
                "property" -> propertiesSB.appendIfRoom(appendStr, limit)
                "function" -> functionSB.appendIfRoom(appendStr, limit)
                "extension property" -> extPropSB.appendIfRoom(appendStr, limit)
                "extension function" -> extFuncSB.appendIfRoom(appendStr, limit)
                "inheritor" -> inheritorsSB.appendIfRoom(appendStr, limit)
            }
        }

        memberSBMap["Properties:"] = propertiesSB.toString()
        memberSBMap["Functions:"] = functionSB.toString()
        memberSBMap["Extension Properties:"] = extPropSB.toString()
        memberSBMap["Extension Functions:"] = extFuncSB.toString()
        memberSBMap["Inheritors:"] = inheritorsSB.toString()

        return memberSBMap
    }

    private fun retrieveMembers(): ArrayList<KotlinMember> {
        val memberArray: ArrayList<KotlinMember> = arrayListOf()

        document.select("div.declarations")
            .map { KotlinMember(it, name) }
            .forEach {
                memberArray.add(it)
            }

        return memberArray
    }

    private fun initializeType() {
        val mainPageDiv = document.selectFirst("div.node-page-main")
        val signatureDiv = mainPageDiv.selectFirst("div.signature")
            ?: throw Exception("Unable to find signature!")

        val keywordList = signatureDiv.select("span.keyword")

        val keywordElement = when (keywordList.size) {
            0 -> throw Exception("Unable to find keyword elements in the div signature")
            1 -> keywordList[0]
            else -> keywordList[1]
        }

        type = keywordElement.text().capitalize()
    }

}