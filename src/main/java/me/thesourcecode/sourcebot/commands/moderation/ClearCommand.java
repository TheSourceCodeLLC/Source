package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceClear;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.concurrent.TimeUnit;

public class ClearCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "clear",
            "Clears the specified amount of messages.",
            "<amount> <reason>",
            CommandInfo.Category.MODERATOR)
            .withControlRoles(SourceRole.DEV, SourceRole.DEV_LEAD, SourceRole.ADMIN, SourceRole.OWNER, SourceRole.MODERATOR)
            .asGuildOnly();

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        int messageAmount;
        try {
            messageAmount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid amount of messages to delete!");
            return new MessageBuilder(alert.build(user)).build();
        }
        if (messageAmount <= 0 || messageAmount > 100) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can only clear 1 - 100 messages!");
            return new MessageBuilder(alert.build(user)).build();
        }

        boolean isStaff = SourceRole.ignoresModeration(message.getMember());

        Category devHelp = SourceChannel.HELP_CATEGORY.resolve(source.getJda());
        TextChannel targetChannel = message.getTextChannel();

        if (targetChannel.getParent() != devHelp && !isStaff) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You do not have permission to use the clear command in this channel!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String reason = String.join(" ", args).replaceFirst(args[0], "");

        SourceClear sourceClear = new SourceClear(user.getId(), targetChannel.getId(), messageAmount, reason);
        if (sourceClear.execute()) {
            sourceClear.sendIncidentEmbed();

            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully cleared " + messageAmount + " messages!");
            targetChannel.sendMessage(sAlert.build(user)).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
        }
        return null;
    }
}
