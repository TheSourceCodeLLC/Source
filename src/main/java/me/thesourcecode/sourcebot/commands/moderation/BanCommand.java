package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceBan;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

public class BanCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "ban",
            "Bans the specified user.",
            "<@user|id|name> <reason>",
            CommandInfo.Category.MODERATOR)
            .withControlRoles(SourceRole.STAFF_MOD);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);

        CommonAlerts alert = new CommonAlerts();

        if (target == null) {
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();

        // Checks if the target is a bot/fake/themselves
        MessageEmbed invalidUser = alert.invalidUser(user, target, "ban");
        if (invalidUser != null) return new MessageBuilder(invalidUser).build();

        String reason = String.join(" ", args).replaceFirst(args[0], "");

        SourceBan sourceBan = new SourceBan(user.getId(), targetUser.getId(), reason);
        sourceBan.sendIncidentEmbed();
        sourceBan.execute();

        // Sends a success message
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully banned " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
