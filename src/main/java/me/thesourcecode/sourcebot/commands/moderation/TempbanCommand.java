package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceTempban;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class TempbanCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "tempban",
            "Tempbans the specified user.",
            "<@user|id|name> <duration + s|m|h|d> <reason>",
            CommandInfo.Category.MODERATOR
    ).withControlRoles(SourceRole.STAFF_MOD);

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
        MessageEmbed invalidUser = alert.invalidUser(user, target, "tempban");
        if (invalidUser != null) return new MessageBuilder(invalidUser).build();

        // Gets the duration integer and the duration type
        long duration;
        String durationType;
        String[] types = {"s", "m", "h", "d"};
        try {
            duration = Integer.parseInt(args[1].replaceAll("[^\\d.]", ""));
            durationType = args[1].replaceAll("[0-9]", "");
        } catch (Exception ex) {
            return new MessageBuilder(alert.invalidDuration(user, "")).build();
        }

        // Checks if duration is less than or equal to 0 and checks if the duration type is valid
        if (duration <= 0) {
            return new MessageBuilder(alert.invalidDuration(user, "time")).build();
        } else if (!Arrays.asList(types).contains(durationType)) {
            return new MessageBuilder(alert.invalidDuration(user, "type")).build();
        }

        // Converts the type to the full type (Ex: s -> Seconds)
        String fullType = Utility.convertDurationType(durationType);

        // Combines the duration with the full type (Ex: 2 Seconds);
        String durationString = duration + " " + (duration == 1 ? fullType.substring(0, fullType.length() - 1) : fullType);

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        SourceTempban sourceTempban = new SourceTempban(user.getId(), targetUser.getId(), durationString, reason);
        if (sourceTempban.execute()) {
            sourceTempban.sendIncidentEmbed();


            // Sends a success embed
            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully tempbanned " + targetUser.getAsTag() + "!");

            return new MessageBuilder(sAlert.build(user)).build();
        }
        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setTitle("Error!").setDescription("I could not tempban that user!");
        return new MessageBuilder(cAlert.build(user)).build();
    }
}