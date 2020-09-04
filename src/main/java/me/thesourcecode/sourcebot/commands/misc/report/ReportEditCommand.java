package me.thesourcecode.sourcebot.commands.misc.report;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;

public class ReportEditCommand extends Command {

    private static CommandInfo INFO = new CommandInfo("edit",
            "Allows a user to edit the reason of their report",
            "<report id> <new reason>",
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

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid report id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();

        DatabaseManager dbManager = source.getDatabaseManager();
        MongoCollection userReports = dbManager.getCollection("UserReports");
        Document query = new Document("REPORT_ID", id);

        Document report = (Document) userReports.find(query).first();
        if (report == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid report id!");
            return new MessageBuilder(alert.build(user)).build();
        }
        String reporterId = report.getString("USER_ID");
        User reporter = source.getJda().getUserById(reporterId);

        if (!reporterId.equalsIgnoreCase(user.getId()) && Collections.disjoint(SourceRole.getRolesFor(member), Arrays.asList(SourceRole.STAFF))) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You do not have permission to edit that report!");
            return new MessageBuilder(alert.build(user)).build();
        }

        Bson updated = new Document("$set", new Document("REASON", reason));
        userReports.updateOne(report, updated);

        String messageId = report.getString("MESSAGE_ID");
        TextChannel reportChannel = SourceChannel.REPORTS.resolve(source.getJda());
        Message reportMessage = reportChannel.retrieveMessageById(messageId).complete();

        if (reportMessage != null) {
            if (reportMessage.getEmbeds().size() != 0) {
                MessageEmbed foundEmbed = reportMessage.getEmbeds().get(0);
                String description = foundEmbed.getDescription();
                String newDescription = description.substring(0, description.indexOf("**Reason:**")).trim();
                newDescription += "\n**Reason:** " + reason;

                CriticalAlert alert = new CriticalAlert();
                alert.setDescription(newDescription)
                        .setAuthor(foundEmbed.getAuthor().getName(), foundEmbed.getAuthor().getUrl(), foundEmbed.getAuthor().getProxyIconUrl())
                        .setFooter(foundEmbed.getFooter().getText(), foundEmbed.getFooter().getIconUrl())
                        .setTimestamp(foundEmbed.getTimestamp());
                reportMessage.editMessage(alert.build(null)).queue();

            }
        }
        if (reporter != null && reporter.hasPrivateChannel()) {
            PrivateChannel pChannel = reporter.openPrivateChannel().complete();
            for (Message foundMessage : pChannel.getIterableHistory().cache(false).complete()) {
                if (foundMessage.getAuthor() != source.getJda().getSelfUser()) continue;
                if (foundMessage.getEmbeds().size() == 0) continue;
                MessageEmbed embed = foundMessage.getEmbeds().get(0);
                if (embed.getAuthor() == null || embed.getDescription() == null) continue;
                if (embed.getAuthor().getName().equalsIgnoreCase("Report | Id: " + id)) {
                    String description = embed.getDescription();
                    String newDescription = description.substring(0, description.indexOf("**Reason:**")).trim();
                    newDescription += "\n**Reason:** " + reason;

                    CriticalAlert newReport = new CriticalAlert();
                    newReport.setDescription(newDescription)
                            .setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getProxyIconUrl())
                            .setFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl())
                            .setTimestamp(embed.getTimestamp());

                    foundMessage.editMessage(newReport.build(null)).queue();
                    break;
                }
            }
        }

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully updated the reason for report id: `" + id + "`!");
        return new MessageBuilder(alert.build(user)).build();
    }
}
