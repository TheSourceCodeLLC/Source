package net.sourcebot.module.documentation.objects.impl

import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.utility.*
import org.jsoup.nodes.Document

/**
 * This class contains the information of a Type from the Kotlin Documentation, an example of a [KotlinType] is a
 * class or an interface.
 *
 * @property document The Jsoup Document which contains the information about the [KotlinType]
 * @property mainPageDiv The div with a class of node-page-main
 * @property memberArray The [KotlinMember] apart of this [KotlinType]
 */
class KotlinType(
    private val document: Document,
) : KotlinInformation() {

    override val url: String = document.baseUri()
    override var name: String = document.selectFirst("h1").text()

    override val type: String by lazy {
        retrieveType()
    }
    override val description: String by lazy {
        retrieveDescription(mainPageDiv)
    }
    override val tags: ArrayList<String> by lazy {
        retrieveTags(mainPageDiv)
    }

    private val mainPageDiv = document.selectFirst("div.node-page-main")
    private val memberArray: ArrayList<KotlinMember> by lazy { retrieveMembers() }


    /**
     * Retrieves all found [KotlinMember]s, apart of this [KotlinType], with a specific name
     *
     * @param query The string that is compared against the [KotlinMember]'s name
     * @return All of the found [KotlinMember]s
     */
    fun searchMembers(query: String): ArrayList<KotlinMember> {
        val modifiedQuery = query.replace("#", ".")
        val foundMembersArray: ArrayList<KotlinMember> = arrayListOf()

        memberArray.filter { it.name.equals(modifiedQuery, true) }
            .forEach { foundMembersArray.add(it) }

        return foundMembersArray
    }

    /**
     * @see KotlinInformation for information about this function
     */
    override fun createResponse(): DocResponse {
        val stringLengthLimit = 300
        val paramHeader = document.selectFirst("h3#parameters")

        val hyperlink = MarkdownUtil.maskedLink(name, url)
        val docResponse = DocResponse()

        docResponse.setAuthor("Kotlin Documentation", null, iconUrl)
        docResponse.setDescription("**__${hyperlink}__**\n$description")

        if (paramHeader != null) {
            val paramParagraphElement = paramHeader.nextElementSibling()
            val paramDesc = paramParagraphElement.anchorsToHyperlinks(baseUrl)
                .toMarkdown()
                .truncate(stringLengthLimit)

            docResponse.addField("Parameters:", paramDesc, false)
        }

        retrieveMemberStringMap(stringLengthLimit)
            .forEach { (fieldName, fieldDescription) ->
                if (fieldDescription.isBlank()) return@forEach
                docResponse.addField(fieldName, fieldDescription, false)
            }

        return docResponse
    }

    /**
     * @see KotlinInformation for information about this function
     */
    override fun retrieveType(): String {
        val mainPageDiv = document.selectFirst("div.node-page-main")
        val signatureDiv = mainPageDiv.selectFirst("div.signature")
            ?: throw Exception("Unable to find signature!")

        val keywordList = signatureDiv.select("span.keyword")

        val keywordElement = when (keywordList.size) {
            0 -> throw Exception("Unable to find keyword elements in the div signature")
            1 -> keywordList[0]
            else -> keywordList[1]
        }

        return keywordElement.text().capitalize()
    }

    /**
     * Retrieves all [KotlinMember]s apart of this [KotlinType] and sorts them based on their type
     *
     * @param limit The [StringBuilder] length limit
     * @return A map which contains all [KotlinMember] types with their found [KotlinMember] names
     */
    private fun retrieveMemberStringMap(limit: Int): Map<String, String> {
        val memberSBMap: MutableMap<String, String> = mutableMapOf()

        val propertiesSB = StringBuilder()
        val functionSB = StringBuilder()
        val extPropSB = StringBuilder()
        val extFuncSB = StringBuilder()
        val inheritorsSB = StringBuilder()

        memberArray.forEach {
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

    /**
     * Retrieves all [KotlinMember]s apart of this [KotlinType]
     *
     * @return An [ArrayList] of all of the found [KotlinMember]
     */
    private fun retrieveMembers(): ArrayList<KotlinMember> {
        val memberArray: ArrayList<KotlinMember> = arrayListOf()

        document.select("div.declarations")
            .map { KotlinMember(it, name) }
            .forEach {
                memberArray.add(it)
            }

        return memberArray
    }


}