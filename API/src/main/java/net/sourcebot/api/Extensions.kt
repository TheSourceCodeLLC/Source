package net.sourcebot.api

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.Arguments
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.urlEncoded(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset)

fun Arguments.nextMember(guild: Guild): Member? {
    val target = next()?.replace("<@(\\d+)>".toRegex(), "$1") ?: return null
    try {
        val byId = guild.getMemberById(target)
        if (byId != null) return byId
    } catch (ignored: Throwable) {
    }
    try {
        val byTag = guild.getMemberByTag(target)
        if (byTag != null) return byTag
    } catch (ignored: Throwable) {
    }
    try {
        val byEffectiveName = guild.getMembersByEffectiveName(target, true)
        if (byEffectiveName.isNotEmpty()) return byEffectiveName[0]
    } catch (ex: Throwable) {
    }
    return null
}

fun Arguments.nextMember(guild: Guild, error: String) =
    nextMember(guild) ?: throw InvalidSyntaxException(error)

fun Arguments.nextRole(guild: Guild): Role? {
    val target = next()?.replace("<@&(\\d+)>".toRegex(), "$1") ?: return null
    try {
        val byId = guild.getRoleById(target)
        if (byId != null) return byId
    } catch (ignored: Throwable) {
    }
    try {
        val byName = guild.getRolesByName(target, true)
        if (byName.isNotEmpty()) return byName[0]
    } catch (ignored: Throwable) {
    }
    return null
}

fun Arguments.nextRole(guild: Guild, error: String) =
    nextRole(guild) ?: throw InvalidSyntaxException(error)