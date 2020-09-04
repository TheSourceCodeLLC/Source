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
import java.util.concurrent.ConcurrentHashMap;

import static me.thesourcecode.sourcebot.api.entity.SourceChannel.devHelpAndCommands;

public class JavaCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "java",
            "Pulls information from the Java Documentation.",
            "(version) (query)",
            CommandInfo.Category.DEVELOPMENT)
            .withUsageChannels(devHelpAndCommands);
    private final ConcurrentHashMap<Integer, JenkinsHandler> javadocCache = new ConcurrentHashMap<>();

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        MessageChannel textChannel = message.getChannel();
        User user = message.getAuthor();
        int version = 13;

        // Checks if args is equal to 0 and if so sends the default embed
        if (args.length == 0) {
            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);


            String docUrl = "https://docs.oracle.com/javase/" + version + "/docs/";
            alert.setDescription("You can find the Java " + version + " Documentation at [docs.oracle.com](" + docUrl + ")");
            alert.appendDescription("\nTo query information from the documentation use: `" + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments() + "`");
            MessageEmbed embed = alert.build(user);
            textChannel.sendMessage(embed).queue();
            return null;
        }

        // Gets the version
        String query = args[0];
        if (args.length > 1) {
            try {
                version = Integer.parseInt(args[0]);
                query = args[1];
            } catch (NumberFormatException ignored) {

            }
        }

        // Gets the url to connect to
        String connectionUrl = getConnectionUrl(version);

        JenkinsHandler javadoc;
        if (javadocCache.containsKey(version)) {
            javadoc = javadocCache.get(version);
        } else {
            // Gets the embed icon and title
            String icon = "https://upload.wikimedia.org/wikipedia/en/thumb/3/30/Java_programming_language_logo.svg/1200px-Java_programming_language_logo.svg.png";
            String embedTitle = "Java " + version + " Javadocs";

            try {
                javadoc = new JenkinsHandler(connectionUrl, icon, embedTitle);
                javadocCache.put(version, javadoc);
            } catch (Exception ex) {
                ex.printStackTrace();
                CriticalAlert notFound = new CriticalAlert();

                notFound.setDescription("I couldn't find the javadocs for the specified java version!");
                return new MessageBuilder(notFound.build(user)).build();
            }
        }

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

            notFound.setDescription("I could not find `" + query + "` in the Java " + version + " Javadocs!" +
                    "\nYou can find the Java " + version + " Documentation at [docs.oracle.com](" + connectionUrl + ")");
            return new MessageBuilder(notFound.build(user)).build();
        }

    }

    private String getConnectionUrl(int version) {
        if (version >= 11) {
            return "https://docs.oracle.com/en/java/javase/" + version + "/docs/api/overview-tree.html";
        } else {
            return "https://docs.oracle.com/javase/" + version + "/docs/api/allclasses-noframe.html";
        }
    }

}
