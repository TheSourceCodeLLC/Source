package net.sourcebot.module.freegames.`object`

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

data class Game(
    val name: String,
    val url: String,
    val platformLogoUrl: String,
    val imageUrl: String,
    val expirationEpoch: Long
) {

    fun toEmbed(): MessageEmbed = EmbedBuilder()
        .setTitle(name)
        .setAuthor(null, null, platformLogoUrl)
        .setThumbnail(platformLogoUrl)
        .setDescription("Free until <t:$expirationEpoch:f>\n\n**[Claim it here!]($url)**")
        .setImage(imageUrl)
        .build()

}