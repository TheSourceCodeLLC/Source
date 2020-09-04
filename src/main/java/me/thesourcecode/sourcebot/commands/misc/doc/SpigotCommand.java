package me.thesourcecode.sourcebot.commands.misc.doc;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.listener.DocSelectionListener;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

import static me.thesourcecode.sourcebot.api.entity.SourceChannel.devHelpAndCommands;

public class SpigotCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "spigot",
            "Pulls information from the Spigot Documentation.",
            "(query)",
            CommandInfo.Category.DEVELOPMENT)
            .withUsageChannels(devHelpAndCommands);
    private JenkinsHandler javadoc;

    public SpigotCommand() {
        String connectionUrl = "https://hub.spigotmc.org/javadocs/spigot/overview-tree.html";
        String imageUrl = "https://avatars0.githubusercontent.com/u/4350249?s=200&v=4";
        String embedTitle = "Spigot Javadocs";

        try {
            javadoc = new JenkinsHandler(connectionUrl, imageUrl, embedTitle);
        } catch (Exception ignored) {
        }
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        MessageChannel textChannel = message.getChannel();
        User user = message.getAuthor();

        if (args.length < 1) {
            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
            alert.setDescription("You can find the Spigot Documentation at [hub.spigotmc.org](https://hub.spigotmc.org/javadocs/spigot/)");
            alert.appendDescription("\nTo query information from the documentation use: `" + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments() + "`");
            MessageEmbed embed = alert.build(user);
            textChannel.sendMessage(embed).queue();
            return null;
        }

        String query = args[0];
        MessageEmbed documentationEmbed = javadoc.search(user, query);

        if (documentationEmbed != null) {
            Message sentDocMessage = message.getChannel().sendMessage(documentationEmbed).complete();

            if (documentationEmbed.getTitle() != null) {
                DocSelectionListener.docStorageCache.put(user, Map.entry(sentDocMessage, javadoc));
            } else {
                sentDocMessage.addReaction(EmojiParser.parseToUnicode(":x:")).queue();
            }

            return null;
        } else {
            CriticalAlert notFound = new CriticalAlert();

            notFound.setDescription("I could not find `" + query + "` in the Spigot Javadocs!" +
                    "\nYou can find the Spigot Documentation at [hub.spigotmc.org](https://hub.spigotmc.org/javadocs/spigot/)");
            return new MessageBuilder(notFound.build(user)).build();
        }
    }
}
