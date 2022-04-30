package net.sourcebot.module.freegames.data

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

data class Game(
    val name: String,
    val url: String,
    val platform: Platform,
    val imageUrl: String,
    var expirationEpoch: Long = 0
) {

    fun toEmbed(): MessageEmbed = EmbedBuilder()
        .setTitle(name)
        .setThumbnail(platform.getLogo())
        .setDescription(
            if (expirationEpoch > 0) "Offer ends on <t:$expirationEpoch:f>\n\n**[Claim it here!]($url)**"
            else "Free while supplies last!\n\n**[Claim it here!]($url)**"
        )
        .setImage(imageUrl)
        .build()

}

enum class Platform {
    STEAM {
        override fun getLogo(): String =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Steam_icon_logo.svg/2048px-Steam_icon_logo.svg.png"
    },
    EPIC_GAMES {
        override fun getLogo(): String =
            "https://cdn2.unrealengine.com/Unreal+Engine%2Feg-logo-filled-1255x1272-0eb9d144a0f981d1cbaaa1eb957de7a3207b31bb.png"
    };

    abstract fun getLogo(): String
}
