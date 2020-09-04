package me.thesourcecode.sourcebot.commands.moderation;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceMute;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class TempmuteCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "tempmute",
            "Tempbans the specified user.",
            "<@user|id|name> <duration + s|m|h|d> <reason>",
            CommandInfo.Category.MODERATOR)
            .withControlRoles(SourceRole.STAFF_MOD)
            .withAliases("mute", "m");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);

        CommonAlerts commonAlerts = new CommonAlerts();

        if (target == null) {
            return new MessageBuilder(commonAlerts.invalidUser(user)).build();
        }

        User targetUser = target.getUser();

        // Checks if the target is a bot/fake/themselves
        MessageEmbed invalidUser = commonAlerts.invalidUser(user, target, "tempmute");

        if (invalidUser != null) {
            return new MessageBuilder(invalidUser).build();
        } else if (SourceRole.getRolesFor(target).contains(SourceRole.MUTED)) {
            CriticalAlert cAlert = new CriticalAlert();
            cAlert.setTitle("Invalid Target!").setDescription("You may not mute someone who is already muted!");
            return new MessageBuilder(cAlert.build(user)).build();
        }

        // Gets the duration and duration type
        long duration;
        String durationType;
        String[] types = {"s", "m", "h", "d"};
        try {
            duration = Integer.parseInt(args[1].replaceAll("[^\\d.]", ""));
            durationType = args[1].replaceAll("[0-9]", "");
        } catch (Exception ex) {
            return new MessageBuilder(commonAlerts.invalidDuration(user, "")).build();
        }

        // Checks if duration is less than or equal to 0 and checks if the duration type is valid
        if (duration <= 0) {
            return new MessageBuilder(commonAlerts.invalidDuration(user, "time")).build();
        } else if (!Arrays.asList(types).contains(durationType)) {
            return new MessageBuilder(commonAlerts.invalidDuration(user, "type")).build();
        }
        // Converts the type to the full type (Ex: s -> Seconds)
        String fullType = Utility.convertDurationType(durationType);

        // Combines the duration with the full type (Ex: 2 Seconds);
        String durationString = duration + " " + (duration == 1 ? fullType.substring(0, fullType.length() - 1) : fullType);

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        SourceMute sourceMute = new SourceMute(user.getId(), targetUser.getId(), durationString, reason);
        sourceMute.sendIncidentEmbed();
        sourceMute.execute();

        // Sends a success embed
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully tempmuted " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
