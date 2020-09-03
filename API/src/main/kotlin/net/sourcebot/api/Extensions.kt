package net.sourcebot.api

import com.fasterxml.jackson.core.type.TypeReference
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.urlEncoded(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset)
fun String.urlDecoded(charset: Charset = StandardCharsets.UTF_8): String = URLDecoder.decode(this, charset)
inline fun <reified T> typeRefOf(): TypeReference<T> = object : TypeReference<T>() {}