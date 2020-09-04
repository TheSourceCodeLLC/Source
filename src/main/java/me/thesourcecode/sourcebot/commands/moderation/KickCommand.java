package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceKick;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class KickCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "kick",
            "Kicks the specified user.",
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

        // Checks if the provided target is valid
        Member target = Utility.getMemberByIdentifier(source.getGuild(), args[0]);
        CommonAlerts alert = new CommonAlerts();
        if (target == null) {
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();

        // Checks if the user is a bot/themselves/fake
        MessageEmbed invalidUser = alert.invalidUser(user, target, "kick");
        if (invalidUser != null) return new MessageBuilder(invalidUser).build();

        // Gets the reason
        String reason = String.join(" ", args).replaceFirst(args[0], "");

        SourceKick sourceKick = new SourceKick(user.getId(), targetUser.getId(), reason);
        sourceKick.sendIncidentEmbed();
        sourceKick.execute();

        // Creates and sends a success alert
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully kicked " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
