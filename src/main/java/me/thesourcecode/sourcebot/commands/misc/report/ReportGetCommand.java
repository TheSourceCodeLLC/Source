package me.thesourcecode.sourcebot.commands.misc.report;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;


public class ReportGetCommand extends Command {

    private static CommandInfo INFO = new CommandInfo("get",
            "Gets the specified report.",
            "<report id>",
            CommandInfo.Category.GENERAL);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member member = Utility.getMemberByIdentifier(guild, user.getId());

        long reportId;

        try {
            reportId = Long.valueOf(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid report id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        DatabaseManager dbManager = source.getDatabaseManager();
        MongoCollection reports = dbManager.getCollection("UserReports");

        Document query = new Document("REPORT_ID", reportId);
        Document found = (Document) reports.find(query).first();

        if (found == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("I did not find any reports with the id: " + reportId + "!");
            return new MessageBuilder(alert.build(user)).build();
        }

        if (!SourceRole.ignoresModeration(member) && !user.getId().equals(found.getString("USER_ID"))) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not view reports that you did not make!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String reporter = getUserString(source.getJda(), found.getString("USER_ID"));
        String target = getUserString(source.getJda(), found.getString("TARGET_ID"));
        String reason = found.getString("REASON");
        long reportTimeInMillis = found.getLong("TIME");

        Date date = new Date(reportTimeInMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a zzz");

        String dateString = dateFormat.format(date);
        String timeString = timeFormat.format(date);

        String dateTime = dateString + " at " + timeString;

        String reportFormat = "**Reported By:** %s\n" +
                "**Reported User:** %s\n" +
                "**Reason:** %s\n" +
                "**Date & Time:** %s";

        if (found.containsKey("HANDLED_BY")) {
            String handledById = found.getString("HANDLED_BY");

            String stateOfReport = "Processing";
            if (handledById != null) {
                if (handledById.contains("INVALID")) {
                    String handledName = getUserString(source.getJda(), handledById.split("\\s+")[0]);
                    handledName = handledName.substring(0, handledName.lastIndexOf("(")).trim();

                    stateOfReport = "Invalid (Handled By: " + handledName + ")";
                } else {
                    String handledName = getUserString(source.getJda(), handledById);
                    handledName = handledName.substring(0, handledName.lastIndexOf("(")).trim();

                    stateOfReport = "Completed (Handled By: " + handledName + ")";
                }
            }

            reportFormat += "\n\n**State Of Report:** " + stateOfReport;
        }

        CriticalAlert reportAlert = new CriticalAlert();
        reportAlert.setDescription(String.format(reportFormat, reporter, target, reason, dateTime));
        message.getChannel().sendMessage(reportAlert.build(user, "Report | Id: " + reportId)).queue();

        return null;
    }

    private String getUserString(JDA jda, String id) {
        try {
            User user = jda.getUserById(id);
            return user.getAsTag() + " (" + user.getId() + ")";
        } catch (NullPointerException ex) {
            return id;
        }
    }
}
