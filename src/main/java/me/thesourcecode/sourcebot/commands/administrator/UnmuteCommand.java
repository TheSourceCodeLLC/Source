package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnmuteCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "unmute",
            "Unmutes the specified user.",
            "<@user|id|name> <reason>",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);

        // Checks if the target exists
        if (target == null) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();
        DatabaseManager databaseManager = source.getDatabaseManager();

        // Gets the punishment handler document for the target
        Document found = (Document) databaseManager.getCollection("PunishmentHandler").find(new Document("ID", targetUser.getId()).append("TYPE", "TEMPMUTE")).first();
        if (found == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("That user is not muted!");
            return new MessageBuilder(alert.build(user)).build();
        }

        // Adds all of the target's previous roles
        List<Role> roles = ((ArrayList<String>) found.get("ROLES")).stream()
                .map(guild::getRoleById).filter(Objects::nonNull)
                .collect(Collectors.toList());
        guild.modifyMemberRoles(target, roles).queue();

        databaseManager.getCollection("PunishmentHandler").deleteOne(found);

        String reason = String.join(" ", args).replaceFirst(args[0], "");

        long caseId = databaseManager.getCollection("IncidentReports").countDocuments() + 1;

        // Creates and sends the unmute incident embed
        SuccessAlert iSAlert = new SuccessAlert();
        iSAlert.setDescription("**Unmuted By:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                "**Unmuted User:** " + targetUser.getAsTag() + " (" + targetUser.getId() + ")\n" +
                "**Reason:** " + reason);
        TextChannel incidentlog = SourceChannel.INCIDENTS.resolve(source.getJda());
        incidentlog.sendMessage(new MessageBuilder(iSAlert.build(targetUser, "Unmute | Id: " + caseId)).build()).queue();

        // DMs the target the incident report
        targetUser.openPrivateChannel().complete().sendMessage(iSAlert.build(targetUser, "Unmute | Id: " + caseId)).queue();


        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        LocalDateTime localDate = LocalDateTime.now();

        // Creates the mongo document for the incident report
        Document mongoReport = new Document("CASE_ID", caseId)
                .append("TYPE", "UNMUTE")
                .append("DATE_TIME", dateFormat.format(localDate))
                .append("STAFF_ID", user.getId())
                .append("TARGET_ID", targetUser.getId())
                .append("REASON", reason)
                .append("TIME_IN_MS", System.currentTimeMillis());
        databaseManager.getCollection("IncidentReports").insertOne(mongoReport);

        // Sends the incident report
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully unmuted " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
