package net.sourcebot.module.documentation.handler

import net.sourcebot.module.documentation.handler.KotlinHandler.KotlinCache
import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.objects.impl.KotlinMember
import net.sourcebot.module.documentation.objects.impl.KotlinType
import org.jsoup.Jsoup

/**
 * This class handles the searching of the Kotlin Documentation and the caching of it
 *
 * @property baseUrl The url to the Kotlin Standard Library documentation site
 * @property typeUrlMap A [Map] which contains the typeUrls and typeNames mapped respectively to each other
 * @property cache The [KotlinCache] object
 */
class KotlinHandler {

    private val baseUrl = "https://kotlinlang.org/api/latest/jvm/stdlib"
    private val typeUrlMap: Map<String, String> by lazy { retrieveObjectUrlMap() }
    private val cache = KotlinCache()

    /**
     * Retrieves an [ArrayList] of [KotlinInformation] based on a given query
     *
     * @param query A [String] of what the user is looking to retrieve from the kotlin docs
     * @return An [ArrayList] of all of the found [KotlinInformation]
     */
    fun search(query: String): ArrayList<KotlinInformation> {
        val modifiedQuery = query.replace("#", ".")
            .removeSuffix(".")
            .removeSuffix("()")
            .toLowerCase()

        val foundInformation = arrayListOf<KotlinInformation>()
        val queryArgs = modifiedQuery.split(".").toTypedArray()

        var foundTypeName = ""
        queryArgs.forEach {
            typeUrlMap.filter { (_, typeName) -> typeName.equals(it, true) }
                .forEach { (_, typeName) ->
                    foundTypeName = typeName
                }
        }

        if (foundTypeName.isNotEmpty()) {
            val foundType = retrieveType(foundTypeName)
            val memberName = modifiedQuery.substringAfterLast(foundTypeName.toLowerCase()).removePrefix(".")

            if (memberName.isNotEmpty()) {
                foundInformation.addAll(foundType.searchMembers(memberName))
            } else {
                foundInformation.add(foundType)
            }
        }

        return foundInformation
    }

    /**
     * Retrieves all [KotlinType]s with a given name
     *
     * @param typeName The name of the [KotlinType] being searched for
     * @return An [ArrayList] of all of the found [KotlinType]s
     */
    fun searchTypes(typeName: String): ArrayList<KotlinType> {
        val resultMap: ArrayList<KotlinType> = arrayListOf()

        typeUrlMap.filter { (_, mapTypeName) -> return@filter mapTypeName.equals(typeName, true) }
            .forEach { (mapTypeUrl, mapTypeName) ->

                if (cache.hasInformation(mapTypeName)) {
                    resultMap.add(cache.getInformation(mapTypeName) as KotlinType)
                    return@forEach
                }

                val htmlDocument = Jsoup.connect(mapTypeUrl)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .get()

                val type = KotlinType(htmlDocument)
                resultMap.add(type)
                cache.putInformation(type)
            }

        return resultMap
    }

    /**
     * Retrieves the first [KotlinType] with a given name
     *
     * @param typeName The name of the [KotlinType] being searched for
     * @return The found [KotlinType]
     * @throws Exception If no [KotlinType] is found
     */
    fun retrieveType(typeName: String): KotlinType {
        val foundTypes = searchTypes(typeName)
        if (foundTypes.isEmpty()) {
            throw Exception("Failed to find a KotlinType with the name of $typeName")
        }

        return searchTypes(typeName)[0]
    }

    /**
     * Retrieves the first [KotlinMember] with a given name in a given [KotlinType]
     *
     * @param type The [KotlinType] being searched in
     * @param memberName The name of the [KotlinMember] being searched for
     * @return The found [KotlinMember]
     * @throws Exception If no [KotlinMember] is found
     */
    fun retrieveMember(type: KotlinType, memberName: String): KotlinMember {
        val foundMembers = type.searchMembers(memberName)
        if (foundMembers.isEmpty()) {
            throw Exception("Failed to find a KotlinMember in the given KotlinType with the name of $memberName")
        }
        return foundMembers[0]
    }

    /**
     * Retrieves the first [KotlinMember] with a given name in a [KotlinType] with a given name
     *
     * @param typeName The [KotlinType]'s name being searched in
     * @param memberName The name of the [KotlinMember] being searched for
     * @return The found [KotlinMember]
     * @throws Exception If no [KotlinMember] is found
     */
    fun retrieveMember(typeName: String, memberName: String): KotlinMember {
        return retrieveMember(retrieveType(typeName), memberName)
    }

    /**
     * Retrieves the names and urls to all of the types in the Kotlin standard library and then stores them in
     * a [Map]
     *
     * @return A [Map] of type urls mapped to their respective names
     */
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

    /**
     * This class caches [KotlinInformation]
     *
     * @property kotlinCacheMap The map which stores the [KotlinInformation] name and it's respective query
     */
    private class KotlinCache {

        private val kotlinCacheMap: MutableMap<String, KotlinInformation> = mutableMapOf()

        /**
         * Checks if a given query has a cached [KotlinInformation]
         *
         * @param name The name being checked
         * @return true or false depending on whether or not the map contains the query as a key
         */
        fun hasInformation(name: String): Boolean {
            return kotlinCacheMap.containsKey(name.toLowerCase())
        }

        /**
         * Adds [KotlinInformation] to the [kotlinCacheMap]
         *
         * @param kotlinInfo The [KotlinInformation] being added
         */
        fun putInformation(kotlinInfo: KotlinInformation) {
            kotlinCacheMap[kotlinInfo.name.toLowerCase()] = kotlinInfo
        }

        /**
         * Retrieves [KotlinInformation] from the cache based on a given query
         *
         * @param name The name being searched
         * @return The found [KotlinInformation] or null
         */
        fun getInformation(name: String): KotlinInformation? {
            return kotlinCacheMap[name.toLowerCase()]
        }
    }

}