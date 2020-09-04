package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;

public class PollCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "poll",
            "Sends a poll to the specified chanel.",
            "(embed) (#channel) <message>",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        Guild guild = source.getGuild();

        boolean isEmbed = args[0].equalsIgnoreCase("embed");
        boolean isOriginalChannel = true;

        String potentialChannelId = args[isEmbed ? 1 : 0].replaceAll("[^0-9]", "");
        MessageChannel targetChannel = message.getChannel();
        if (!potentialChannelId.isEmpty()) {
            targetChannel = guild.getTextChannelById(potentialChannelId);
            if (targetChannel == null) {
                targetChannel = message.getChannel();
            } else {
                isOriginalChannel = false;
            }
        }

        int argsSkip = (isEmbed ? 1 : 0) + (isOriginalChannel ? 0 : 1);
        String pollMessageStr = String.join(" ", Arrays.copyOfRange(args, argsSkip, args.length));

        Message poll;
        if (isEmbed) {
            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
            alert.setDescription(pollMessageStr);

            poll = targetChannel.sendMessage(alert.build()).complete();
        } else {
            poll = targetChannel.sendMessage(pollMessageStr).complete();
        }

        poll.addReaction("✅").complete();
        poll.addReaction("❌").complete();

        if (targetChannel != message.getChannel()) {
            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully sent a poll to " + targetChannel.getName() + "!");
            return new MessageBuilder(sAlert.build(message.getAuthor())).build();
        } else {
            message.delete().queue();
            return null;
        }
    }
}
