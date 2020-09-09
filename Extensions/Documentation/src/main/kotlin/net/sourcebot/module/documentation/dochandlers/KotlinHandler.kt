package net.sourcebot.module.documentation.dochandlers

import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.objects.impl.KotlinMember
import net.sourcebot.module.documentation.objects.impl.KotlinType
import org.jsoup.Jsoup

class KotlinHandler {

    private val baseUrl = "https://kotlinlang.org/api/latest/jvm/stdlib"
    private val typeUrlMap: Map<String, String> by lazy { retrieveObjectUrlMap() }
    private val cache = KotlinCache()

    fun search(query: String): ArrayList<KotlinInformation> {
        val modifiedQuery = query.replace("#", ".")
            .removeSuffix(".")
            .removeSuffix("()")
            .toLowerCase()

        val foundInformation = arrayListOf<KotlinInformation>()
        val queryArgs = modifiedQuery.split(".").toTypedArray()

        val retrievedTypeList = searchTypes(queryArgs[0])

        if (retrievedTypeList.isEmpty()) {
            throw Exception("Failed to find a type with the name of $query")
        }

        when (queryArgs.size) {
            1 -> {
                foundInformation.addAll(retrievedTypeList)
            }
            2 -> {
                val foundType = retrievedTypeList[0]
                foundInformation.addAll(foundType.searchMembers(queryArgs[1]))
            }
            else -> {
                var foundType = retrievedTypeList[0]
                var foundNewType = false
                val queryArgsNoName = queryArgs.copyOfRange(1, queryArgs.size)

                for (x in queryArgsNoName.indices) {
                    if (!foundNewType && x != 0) {
                        throw Exception("Failed to find any results for the query $query")
                    }

                    foundNewType = false

                    val arg = queryArgsNoName[x]
                    val foundMembers = foundType.searchMembers(arg)

                    if (foundMembers.isEmpty()) {
                        throw Exception("Failed to find any results for the query $query")
                    }

                    if (x == queryArgsNoName.size - 1) {
                        foundInformation.addAll(foundMembers)
                        break
                    }

                    foundMembers.filter { it.type.equals("inheritor", true) }
                        .forEach {
                            foundType = retrieveType(it.name)
                            foundNewType = true
                        }
                }

            }
        }

        foundInformation.filter { it.type.equals("inheritor", true) }
            .forEach {
                val retrievedType = retrieveType(it.name)
                foundInformation.remove(it)
                foundInformation.add(retrievedType)
            }

        return foundInformation
    }

    fun searchTypes(query: String): ArrayList<KotlinType> {
        val resultMap: ArrayList<KotlinType> = arrayListOf()

        typeUrlMap.filter { (_, typeName) -> return@filter typeName.equals(query, true) }
            .forEach { (typeUrl, typeName) ->

                if (cache.hasInformation(typeName)) {
                    resultMap.add(cache.getInformation(typeName) as KotlinType)
                    return@forEach
                }

                val htmlDocument = Jsoup.connect(typeUrl)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .get()

                val type = KotlinType(htmlDocument)
                resultMap.add(type)
                cache.putInformation(typeName, type)
            }

        return resultMap
    }

    fun retrieveType(typeName: String): KotlinType {
        val foundTypes = searchTypes(typeName)
        if (foundTypes.isEmpty()) throw Exception("Failed to find a type with the name of $typeName")

        return searchTypes(typeName)[0]
    }

    fun retrieveMember(typeName: String, memberName: String): KotlinMember {
        return retrieveMember(retrieveType(typeName), memberName)
    }

    fun retrieveMember(type: KotlinType, memberName: String): KotlinMember {
        val foundMembers = type.searchMembers(memberName)
        if (foundMembers.isEmpty()) throw Exception("Failed to find a member with the name of $memberName")
        return foundMembers[0]
    }

    private fun retrieveObjectUrlMap(): Map<String, String> {
        val returnMap: MutableMap<String, String> = mutableMapOf()

        try {
            val htmlDocument = Jsoup.connect("$baseUrl/alltypes/")
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()

            htmlDocument.select("div.declarations")
                .filter { it.selectFirst("h5 > a") != null }
                .map { it.selectFirst("h5 > a") }
                .forEach {
                    val href = it.attr("href").replace("../", "")
                    val typeUrl = "$baseUrl/$href"
                    val typeName = it?.text()?.substringAfterLast(".") ?: return@forEach

                    returnMap[typeUrl] = typeName
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return returnMap
    }

    private class KotlinCache {
        private val docCache: MutableMap<String, KotlinInformation> = mutableMapOf()

        fun hasInformation(query: String): Boolean {
            return docCache.containsKey(query.toLowerCase())
        }

        fun putInformation(query: String, response: KotlinInformation) {
            docCache[query.toLowerCase()] = response
        }

        fun getInformation(query: String): KotlinInformation? {
            return docCache[query.toLowerCase()]
        }
    }

}