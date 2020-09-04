package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public class BroadcastCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "broadcast",
            "Broadcasts a message to the current or specified channel.",
            "(embed) (#channel) <message>",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER)
            .withAliases("bc", "say");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        MessageChannel targetChannel = message.getChannel();
        int argsSkip = args[0].equalsIgnoreCase("embed") ? 1 : 0;
        int getChannel = 0;
        if (args[0].equalsIgnoreCase("embed")) getChannel = 1;
        if (message.getMentionedChannels().size() > 0 && args[getChannel].equals(message.getMentionedChannels().get(0).getAsMention())) {
            targetChannel = message.getMentionedChannels().get(0);
            if (args[0].equalsIgnoreCase("embed")) {
                argsSkip = 2;
            } else argsSkip = 1;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = argsSkip; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String msg = sb.toString().trim();

        if (args[0].equalsIgnoreCase("embed")) {
            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
            alert.setDescription(msg);
            targetChannel.sendMessage(alert.build()).queue();

            if (targetChannel != message.getChannel()) {
                SuccessAlert sAlert = new SuccessAlert();
                sAlert.setDescription("You have successfully broadcasted a message to `" + targetChannel.getName() + "`!");
                return new MessageBuilder(sAlert.build()).build();
            } else {
                if (message.getChannelType() != ChannelType.PRIVATE) message.delete().queue();
                return null;
            }
        } else {
            targetChannel.sendMessage(msg).queue();
            if (message.getChannelType() != ChannelType.PRIVATE) message.delete().queue();
            return null;
        }

    }
}
