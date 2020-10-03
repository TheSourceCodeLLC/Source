package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceSoftban;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

public class SoftbanCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "softban",
            "Softbans the specified user.",
            "<@user|id|name> <reason>",
            CommandInfo.Category.MODERATOR)
            .withControlRoles(SourceRole.STAFF_MOD).withAliases("sb");

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

        // Checks if the target is non existant
        if (target == null) {
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();

        // Checks if the target is a bot/fake/themselves
        MessageEmbed invalidUser = alert.invalidUser(user, target, "softban");
        if (invalidUser != null) return new MessageBuilder(invalidUser).build();

        String reason = String.join(" ", args).replaceFirst(args[0], "");

        SourceSoftban sourceSoftban = new SourceSoftban(user.getId(), targetUser.getId(), reason);
        if (sourceSoftban.execute()) {
            sourceSoftban.sendIncidentEmbed();

            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully softbanned " + targetUser.getAsTag() + "!");

            return new MessageBuilder(sAlert.build(user)).build();
        }
        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setTitle("Error!").setDescription("I could not softban that user!");
        return new MessageBuilder(cAlert.build(user)).build();
    }

}