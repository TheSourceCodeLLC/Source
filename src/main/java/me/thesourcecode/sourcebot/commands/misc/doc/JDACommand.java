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

public class JDACommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "jda",
            "Pulls information from the JDA Documentation.",
            "(query)",
            CommandInfo.Category.DEVELOPMENT)
            .withUsageChannels(devHelpAndCommands);

    private JenkinsHandler javadoc;

    public JDACommand() {
        String imageUrl = "https://camo.githubusercontent.com/f2e0860a3b1a34658f23a8bcea96f9725b1f8a73/68747470733a2f2f692e696d6775722e636f6d2f4f4737546e65382e706e67";
        String embedTitle = "JDA Javadocs";

        try {
            String connectionURL = "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html";
            javadoc = new JenkinsHandler(connectionURL, imageUrl, embedTitle);
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
            alert.setDescription("You can find the JDA Documentation at [ci.dv8tion.net](https://ci.dv8tion.net/job/JDA/javadoc/)");
            alert.appendDescription("\nTo query information from the documentation use: `" + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments() + "`");
            MessageEmbed embed = alert.build(user);
            textChannel.sendMessage(embed).queue();
            return null;
        }


        String query = args[0].replace("#", ".");
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

            notFound.setDescription("I could not find `" + query + "` in the JDA Javadocs!" +
                    "\nYou can find the JDA Documentation at [docs.oracle.com](https://ci.dv8tion.net/job/JDA/javadoc/)");
            return new MessageBuilder(notFound.build(user)).build();
        }
    }
}
