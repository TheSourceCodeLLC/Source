package me.thesourcecode.sourcebot.commands.developer.role;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class RoleRemoveCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "remove",
            "Removes the specified role to the specified user.",
            "<@user|id|name> <1,2,3...> <reason>",
            CommandInfo.Category.DEVELOPER
    ).withUsageChannels(SourceChannel.COMMANDS).withControlRoles(SourceRole.DEV_LEAD, SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        HashMap<Integer, Role> roleList = RoleCommand.getRoleList(source, message);
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);
        if (target == null) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        int roleId;
        try {
            roleId = Integer.parseInt(args[1]);
            if (roleList.get(roleId) == null) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("You did not enter a valid role id!");
                return new MessageBuilder(alert.build(user)).build();
            }
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid role id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        User targetUser = target.getUser();
        Role role = roleList.get(roleId);
        boolean log = false;
        for (SourceRole sRole : RoleCommand.LOG_ROLES) {
            if (sRole.resolve(source.getJda()) == role) log = true;
        }
        if (!target.getRoles().contains(role)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("The user does not have the role you are trying to remove!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String fullTargetName = targetUser.getName() + "#" + targetUser.getDiscriminator();
        String fullUserName = user.getName() + "#" + user.getDiscriminator();

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String reason = sb.toString().trim();

        guild.removeRoleFromMember(target, role).queue();
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully removed the role `" + role.getName() + "` from " + fullTargetName + "!");

        if (log) {
            DatabaseManager databaseManager = source.getDatabaseManager();
            long caseId = databaseManager.getCollection("IncidentReports").countDocuments() + 1;

            ColoredAlert cAlert = new ColoredAlert(SourceColor.RED);
            cAlert.setDescription("**Role Removed By:** " + fullUserName + " (" + user.getId() + ")\n" +
                    "**Role Removed From:** " + fullTargetName + " (" + targetUser.getId() + ")\n" +
                    "**Role Removed:** " + role.getName() + " (" + role.getId() + ")\n" +
                    "**Reason:** " + reason);
            TextChannel incidentlog = SourceChannel.INCIDENTS.resolve(source.getJda());
            incidentlog.sendMessage(new MessageBuilder(cAlert.build(targetUser, "Role Update | Id: " + caseId)).build()).queue();

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            LocalDateTime localDate = LocalDateTime.now();

            Document mongoReport = new Document("CASE_ID", caseId)
                    .append("TYPE", "ROLE UPDATE")
                    .append("ACTION", "REMOVE")
                    .append("DATE_TIME", dateFormat.format(localDate))
                    .append("STAFF_ID", user.getId())
                    .append("TARGET_ID", targetUser.getId())
                    .append("ROLE_ID", role.getId())
                    .append("REASON", reason)
                    .append("TIME_IN_MS", System.currentTimeMillis());
            databaseManager.getCollection("IncidentReports").insertOne(mongoReport);
        }

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
