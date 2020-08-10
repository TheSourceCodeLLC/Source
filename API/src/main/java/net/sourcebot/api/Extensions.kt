package net.sourcebot.api

import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.urlEncoded(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset)