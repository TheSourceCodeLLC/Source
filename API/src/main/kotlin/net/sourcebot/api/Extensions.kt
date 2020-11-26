package net.sourcebot.api

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.response.EmbedResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.WrappedEmbedResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.math.max

fun String.urlEncoded(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset)
fun String.urlDecoded(charset: Charset = StandardCharsets.UTF_8): String = URLDecoder.decode(this, charset)
inline fun <reified T> typeRefOf(): TypeReference<T> = object : TypeReference<T>() {}

fun Member.formatted(): String = user.formatted()
fun User.formatted(): String = "%#s".format(this)

fun Response.asMessage(member: Member) = asMessage(member.user)

@JvmOverloads
fun String.truncate(limit: Int, ellipsis: String = "..."): String =
    if (length <= limit) this else substring(0, limit - ellipsis.length) + ellipsis

fun <A, B> List<A>.zipAll(other: List<B>) =
    (0 until max(size, other.size)).map { getOrNull(it) to other.getOrNull(it) }.toList()

fun EmbedResponse.wrapped(forWho: User) = WrappedEmbedResponse(this.asEmbed(forWho))
fun EmbedResponse.wrapped(forWho: Member) = this.wrapped(forWho.user)

fun <T> T?.ifPresentOrElse(
    ifPresent: (T) -> Unit,
    orElse: () -> T
) = this?.also(ifPresent) ?: orElse()

fun Member.getHighestRole() = roles.getOrNull(0) ?: guild.publicRole

@JvmOverloads
fun Double.round(
    precision: Int,
    mode: RoundingMode = RoundingMode.HALF_UP
) = BigDecimal(this).setScale(precision, mode).toDouble()

fun <U> Message.MentionType.listMatches(
    content: String,
    transformer: (String) -> U?
): List<U> {
    val list = ArrayList<U>()
    pattern.matcher(content).results().forEach {
        list += transformer(it.group(1)) ?: return@forEach
    }
    return list
}

fun <K, V> weakCache(
    expire: Duration = Duration.ofMinutes(10),
    loader: (K) -> V
): LoadingCache<K, V> =
    CacheBuilder.newBuilder().weakKeys().expireAfterWrite(expire)
        .build(object : CacheLoader<K, V>() {
            override fun load(key: K) = loader(key)
        })

fun Member.allRoles() = ArrayList(roles).also { it.add(guild.publicRole) }.toList()

fun formatPlural(amount: Long, unit: String): String {
    return if (amount == 1L) "$amount $unit" else "$amount ${unit}s"
}