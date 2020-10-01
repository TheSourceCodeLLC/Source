package net.sourcebot.api

import com.fasterxml.jackson.core.type.TypeReference
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.response.Response
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.urlEncoded(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset)
fun String.urlDecoded(charset: Charset = StandardCharsets.UTF_8): String = URLDecoder.decode(this, charset)
inline fun <reified T> typeRefOf(): TypeReference<T> = object : TypeReference<T>() {}

fun Member.formatted(): String = user.formatted()
fun User.formatted(): String = "%#s".format(this)

fun Response.asMessage(member: Member) = asMessage(member.user)

@JvmOverloads
fun String.truncate(limit: Int, ellipsis: String = "..."): String =
    if (length <= limit) this else substring(0, limit - ellipsis.length) + ellipsis