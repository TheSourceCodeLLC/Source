package net.sourcebot.module.latex

import com.mongodb.client.MongoCollection
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.latex.command.LatexCommand
import net.sourcebot.module.latex.listener.LatexListener
import org.bson.Document
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class Latex : SourceModule() {
    override fun enable() {
        registerCommands(LatexCommand())
        subscribeEvents(LatexListener())
    }

    companion object {
        private val blockedMacros = listOf(
            "\\newenvironment",
            "\\renewcommand",
            "\\newcommand",
            "\\newcounter",
            "\\def"
        )
        @JvmStatic val DELETE_REACT = "❌"
        @JvmStatic val Database: (Guild) -> MongoCollection<Document> = {
            Source.MONGODB.getCollection(it.id, "latex")
        }

        @JvmStatic fun parse(input: String): BufferedImage {
            blockedMacros.forEach {
                if (!input.contains(it)) return@forEach
                throw DisabledMacroException(it)
            }
            val expression = TeXFormula(input)
            val icon = expression.createTeXIcon(
                TeXConstants.STYLE_DISPLAY, 20f
            )
            val image = BufferedImage(
                icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB
            )
            val graphics = image.createGraphics().also {
                it.color = Color.WHITE
                it.fillRect(0, 0, icon.iconWidth, icon.iconHeight)
            }
            icon.paintIcon(null, graphics, 0, 0)
            return image
        }

        @JvmStatic fun send(author: User, channel: MessageChannel, image: BufferedImage) {
            val input = ByteArrayOutputStream().also {
                ImageIO.write(image, "png", it)
            }.let {
                ByteArrayInputStream(it.toByteArray())
            }
            channel.sendMessage("**${author.name}**:").addFile(input, "latex.png").queue {
                if (channel.type != ChannelType.TEXT) return@queue
                val database = Database(it.guild)
                database.insertOne(
                    Document(
                        mapOf(
                            "author" to author.id,
                            "result" to it.id
                        )
                    )
                )
                it.addReaction(DELETE_REACT).queue()
            }
        }
    }
}

open class LatexException(error: String) : RuntimeException(error)
class DisabledMacroException(macro: String) : LatexException(
    "Macro is disabled: `$macro`!"
)