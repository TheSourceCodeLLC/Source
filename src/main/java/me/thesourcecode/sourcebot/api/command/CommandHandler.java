package me.thesourcecode.sourcebot.api.command;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler {
    private static final CommandMap commands = new CommandMap();
    private static final String prefix = "!";
    private final long deleteAfterAmount = 15;
    private final TimeUnit deleteAfterUnit = TimeUnit.SECONDS;
    private final Pattern pattern = Pattern.compile("(\".*?\"|[^ ]+)");

    public CommandHandler(Source source) {
        Listener listener = source.getListener();
        listener.handle(MessageReceivedEvent.class, event -> {
            Message message = event.getMessage();
            Guild guild = source.getGuild();
            if (guild != null) {
                if (!SourceGuild.isSupported(guild)) {
                    //Ignore non-Source guilds
                    return;
                }
            }
            String content = message.getContentRaw();
            if (!content.startsWith(prefix)) {
                return;
            }
            content = content.replaceFirst(prefix, "");

            Matcher matcher = pattern.matcher(content);
            final String[] args = matcher.results()
                    .map(result -> {
                        final String group = result.group(1);
                        if (group.startsWith("\"") && group.endsWith("\"")) {
                            return group.substring(1, group.length() - 1);
                        }
                        return group;
                    }).toArray(String[]::new);
            if (args.length == 0) return;
            String label = args[0].toLowerCase();
            Command command = commands.getCommand(label);
            if (command == null) {
                return;
            }

            Message response = command.onCommand(source, message, Arrays.copyOfRange(args, 1, args.length));
            if (response != null) {
                if (message.getChannelType() != ChannelType.PRIVATE)
                    message.delete().queueAfter(deleteAfterAmount, deleteAfterUnit);

                message.getChannel().sendMessage(response).queue(m -> {
                    m.delete().queueAfter(deleteAfterAmount, deleteAfterUnit);
                });
            }

        });
    }

    /***
     *
     * @return The prefix used for all commands
     */
    public static String getPrefix() {
        return prefix;
    }

    /***
     *
     * @return The command map for this handler
     */
    public static CommandMap getCommands() {
        return commands;
    }

    /***
     *
     * @param command The command to register
     * @param <T> The type of the command registered
     * @return The registered command
     */
    public final <T extends Command> T registerCommand(T command) {
        return commands.register(command);
    }
}
