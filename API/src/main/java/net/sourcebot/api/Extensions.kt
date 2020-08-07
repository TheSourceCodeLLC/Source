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