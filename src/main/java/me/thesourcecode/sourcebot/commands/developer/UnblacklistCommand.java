package me.thesourcecode.sourcebot.commands.developer;

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

public class UnblacklistCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "unblacklist",
            "Unblacklists the specified user.",
            "<@user|id|name> <reason>",
            CommandInfo.Category.DEVELOPER)
            .withControlRoles(SourceRole.DEVELOPERS_STAFF)
            .withAliases("ubl");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);

        // Checks if the user is null
        if (target == null) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();
        DatabaseManager databaseManager = source.getDatabaseManager();

        // Checks if the user is blacklisted
        Document found = databaseManager.getCollection("PunishmentHandler").find(new Document("ID", targetUser.getId())
                .append("TYPE", "BLACKLIST")
                .append("CATEGORY", "DEVELOPMENT")).first();
        if (found == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("That user is not blacklisted!");
            return new MessageBuilder(alert.build(user)).build();
        }

        // Removes the blacklist role and deletes the punishment handler document
        guild.removeRoleFromMember(target, SourceRole.BLACKLIST.resolve(source.getJda())).complete();
        databaseManager.getCollection("PunishmentHandler").deleteOne(found);

        String reason = String.join(" ", args).replaceFirst(args[0], "");
        long caseId = databaseManager.getCollection("IncidentReports").countDocuments() + 1;

        // Creates and snds the incident report
        SuccessAlert iSAlert = new SuccessAlert();
        iSAlert.setDescription("**Unblacklisted By:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                "**Unblacklisted User:** " + targetUser.getAsTag() + " (" + targetUser.getId() + ")\n" +
                "**Reason:** " + reason);
        TextChannel incidentlog = SourceChannel.INCIDENTS.resolve(source.getJda());
        incidentlog.sendMessage(iSAlert.build(targetUser, "Unblacklist | Id: " + caseId)).queue();


        // Sends the incident report to the target
        PrivateChannel channel = targetUser.openPrivateChannel().complete();
        channel.sendMessage("You have been unblacklisted from the development help channels! See below for more info.").queue();
        targetUser.openPrivateChannel().complete().sendMessage(iSAlert.build(targetUser, "UnBlacklist | Id: " + caseId)).queue();


        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        LocalDateTime localDate = LocalDateTime.now();

        // Creates the mongo document for the incident report
        Document mongoReport = new Document("CASE_ID", caseId)
                .append("TYPE", "UNBLACKLIST")
                .append("DATE_TIME", dateFormat.format(localDate))
                .append("STAFF_ID", user.getId())
                .append("TARGET_ID", targetUser.getId())
                .append("REASON", reason)
                .append("TIME_IN_MS", System.currentTimeMillis());
        databaseManager.getCollection("IncidentReports").insertOne(mongoReport);

        // Sends a success embed
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully unblacklisted " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }

}
