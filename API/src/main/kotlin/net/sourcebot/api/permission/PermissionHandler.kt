package net.sourcebot.api.permission

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.sourcebot.Source
import net.sourcebot.api.allRoles
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.InvalidChannelResponse
import net.sourcebot.api.response.error.NoPermissionDMAllowedResponse
import net.sourcebot.api.response.error.NoPermissionResponse
import org.bson.Document

class PermissionHandler(private val globalAdmins: Set<String>) {
    private val mongodb = Source.MONGODB
    private val dataCache = HashMap<String, PermissionData>()

    private fun computeContext(channel: MessageChannel): Set<String> {
        val context = mutableSetOf<String>()
        return if (channel !is TextChannel) context
        else context.apply {
            add(channel.id)
            if (channel.parent != null) add(channel.parent!!.id)
        }
    }

    private fun hasPermission(
        permissible: Permissible,
        node: String,
        context: Set<String>
    ): Boolean {
        if (permissible is SourceUser && hasGlobalAccess(permissible.id)) return true
        val effective = getEffectiveNodes(node)
        effective.forEach { eff ->
            when (permissible.hasPermission(eff)) {
                true -> return true
                false -> return false
            }
            for (ctx in context) return permissible.hasPermission(eff, ctx) ?: continue
        }
        return false
    }

    private fun hasGlobalAccess(id: String) = id in globalAdmins
    fun hasGlobalAccess(user: User) = hasGlobalAccess(user.id)

    fun hasPermission(
        permissible: Permissible,
        node: String,
        context: String? = null
    ): Boolean = hasPermission(permissible, node, context?.let(::setOf) ?: emptySet())

    fun hasPermission(
        permissible: Permissible,
        node: String,
        channel: MessageChannel? = null
    ): Boolean = hasPermission(permissible, node, channel?.let(::computeContext) ?: emptySet())

    fun memberHasPermission(
        member: Member,
        node: String,
        channel: MessageChannel? = null
    ): Boolean {
        if (member.allRoles().any { it.hasPermission(Permission.ADMINISTRATOR) }) return true
        val subject = getData(member.guild).getUser(member)
        return hasPermission(subject, node, channel)
    }

    fun roleHasPermission(
        role: Role,
        node: String,
        channel: MessageChannel? = null
    ): Boolean {
        if (role.hasPermission(Permission.ADMINISTRATOR)) return true
        val subject = getData(role.guild).getRole(role)
        return hasPermission(subject, node, channel)
    }

    fun checkPermission(
        permissible: Permissible,
        node: String,
        channel: MessageChannel,
        ifPresent: () -> Response
    ): Response =
        if (hasPermission(permissible, node, channel)) ifPresent()
        else getPermissionAlert(true, channel.jda, permissible, node)

    private fun getEffectiveNodes(permission: String): Set<String> =
        LinkedHashSet<String>().apply {
            add(permission)
            addAll(permission.mapIndexed { idx, c ->
                if (c == '.') permission.substring(0..idx) + "*" else null
            }.filterNotNull().distinct().reversed())
            add("*")
        }

    private fun getAllowedContexts(
        permissible: Permissible,
        node: String
    ): Set<String> {
        val allowed = mutableSetOf<String>()
        getEffectiveNodes(node).forEach {
            if (permissible.hasPermission(it) == false) return allowed
            allowed.addAll(permissible.getContexts(it))
        }
        return allowed
    }

    fun getPermissionAlert(
        guildOnly: Boolean, jda: JDA,
        permissible: Permissible, node: String
    ): Response {
        val availableIn = getAllowedContexts(permissible, node)
        return if (availableIn.isEmpty()) {
            if (guildOnly) NoPermissionResponse()
            else NoPermissionDMAllowedResponse()
        } else InvalidChannelResponse(jda, availableIn)
    }

    private fun getData(guild: String): PermissionData = dataCache.computeIfAbsent(guild) {
        PermissionData(mongodb.getDatabase(it))
    }

    fun getData(guild: Guild) = getData(guild.id)

    internal fun getPermissions(document: Document) =
        document.getList("permissions", Document::class.java).map {
            MongoSerial.fromDocument<SourcePermission>(it)
        }.toSet()
}