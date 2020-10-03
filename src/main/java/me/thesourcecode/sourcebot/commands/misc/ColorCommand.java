package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class ColorCommand extends Command {
    private static final CommandInfo INFO = new CommandInfo(
            "color", "Render a color swatch"
    ).withUsageChannels(SourceChannel.COMMANDS);
    private static final int IMAGE_SIZE = 128;

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        try {
            final String hexCode = args[0].toUpperCase();
            final Color color = Color.decode(hexCode);
            final BufferedImage image = new BufferedImage(
                    IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB
            );
            final Graphics graphics = image.getGraphics();
            graphics.setColor(color);
            graphics.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            new MessageBuilder(
                    new ColoredAlert(color) {{
                        setImage("attachment://color.png");
                    }}.build(message.getAuthor())
            ).sendTo(message.getChannel()).addFile(output.toByteArray(), "color.png").queue();
        } catch (Throwable err) {
            err.printStackTrace();
        }
        return null;
    }
}
