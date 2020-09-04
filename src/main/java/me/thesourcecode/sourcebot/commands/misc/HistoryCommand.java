package me.thesourcecode.sourcebot.commands.misc;

import com.google.common.collect.Iterators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.Alert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.bson.Document;

import java.util.Iterator;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.orderBy;

public class HistoryCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "history",
            "Gets the history of the specified user.",
            "(@user|id|name) (page)",
            CommandInfo.Category.GENERAL)
            .withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();

        User targetUser = user;
        String targetId = user.getId();
        int pageNum = 1;
        CommonAlerts commonAlert = new CommonAlerts();

        if (args.length > 0) {
            Member foundMember = Utility.getMemberByIdentifier(guild, args[0]);
            if (foundMember == null) {
                targetId = args[0];

            } else {
                targetId = foundMember.getId();
            }

            // User account could be deleted by still have an incident report
            try {
                targetUser = source.getJda().getUserById(targetId);
                if (targetUser != null && (targetUser.isBot() || targetUser.isFake())) {
                    CriticalAlert alert = new CriticalAlert();
                    alert.setTitle("Uh Oh!").setDescription("You can not check a discord bot's history!");
                    return new MessageBuilder(alert.build(user)).build();
                }
            } catch (NumberFormatException ex) {
                targetUser = null;
            }

            if (args.length > 1) {
                try {
                    pageNum = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    return new MessageBuilder(commonAlert.invalidPage(user)).build();
                }
            }
        }

        DatabaseManager dbManager = source.getDatabaseManager();
        MongoCollection<Document> userPunishPoints = dbManager.getCollection("Punishments");

        MongoCollection<Document> incidentReports = dbManager.getCollection("IncidentReports");
        MongoCollection<Document> reports = dbManager.getCollection("UserReports");

        if (targetUser == null && !collectionContainsUser(incidentReports, targetId) && !collectionContainsUser(reports, targetId)) {
            targetUser = user;
            targetId = user.getId();

            try {
                pageNum = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                return new MessageBuilder(commonAlert.invalidPage(user)).build();
            }
        }

        int incidentPages = getCollectionMaxPages(incidentReports, targetId);
        int reportPages = getCollectionMaxPages(reports, targetId);

        int maxPages = Math.max(incidentPages, reportPages);
        maxPages = maxPages == 0 ? 1 : maxPages;

        if (pageNum > maxPages) {
            return new MessageBuilder(commonAlert.invalidPage(user)).build();
        }

        InfoAlert infoAlert = new InfoAlert();

        Document targetPunishPoints = userPunishPoints.find(new Document("id", targetId)).first();
        String punishPointsString = "**Punishment Points:** ";
        if (targetPunishPoints != null) {
            double punishPoints = targetPunishPoints.getDouble("points");
            punishPointsString += punishPoints;
        } else punishPointsString += "0.0";

        infoAlert.appendDescription(punishPointsString);

        infoAlert.appendDescription("\n\n**Incidents:**");
        addItemToList(incidentReports, infoAlert, targetId, pageNum);

        if (SourceRole.ignoresModeration(guild.getMember(user))) {
            infoAlert.appendDescription("\n\n**Reports:**");
            addItemToList(reports, infoAlert, targetId, pageNum);
        }

        infoAlert.appendDescription("\n\nPage " + pageNum + " of " + maxPages);

        String targetIdentifier = targetUser == null ? targetId : targetUser.getAsTag();
        MessageEmbed messageEmbed = infoAlert.build(targetUser == null ? user : targetUser, targetIdentifier + "'s History");

        message.getChannel().sendMessage(messageEmbed).queue();
        return null;
    }


    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    /**
     * Checks if a user has a document with their id in a specified collection
     *
     * @param collection The collection being searched
     * @param targetId   The id of the user being searched for
     * @return True if a document is found, false if not
     */
    private boolean collectionContainsUser(MongoCollection<Document> collection, String targetId) {
        Document found = collection.find(new Document("TARGET_ID", targetId)).first();

        return found != null;
    }

    /**
     * Gets the number of pages (5 items per page) of documents that contain the user's id
     *
     * @param collection The collection being searched
     * @param targetId   The id of the user being searched for
     * @return The number of pages
     */
    private int getCollectionMaxPages(MongoCollection<Document> collection, String targetId) {
        Document query = new Document("TARGET_ID", targetId);

        long collectionCount = Iterators.size(collection.find(query).iterator());
        return (int) Math.floor((double) collectionCount / 5);
    }

    /**
     * Gets all incidents/reports for a user in a certain range and adds them to the alert
     *
     * @param collection The collection the incidents/reports are being pulled from
     * @param alert      The alert the are being added to
     * @param targetId   The id of the target
     * @param pageNum    The page number
     */
    private void addItemToList(MongoCollection<Document> collection, Alert alert, String targetId, int pageNum) {
        Document query = new Document("TARGET_ID", targetId);

        FindIterable<Document> foundDocumentList = collection.find(query)
                .skip(pageNum == 1 ? 0 : (pageNum * 5) - 5)
                .limit(5)
                .sort(orderBy(ascending("TIME_IN_MS")));

        Iterator<Document> iterator = foundDocumentList.iterator();

        int count = 0;
        while (iterator.hasNext()) {
            Document document = iterator.next();

            String addToDescription;
            if (document.containsKey("CASE_ID")) {
                long caseId = document.getLong("CASE_ID");

                String reason = document.getString("REASON").trim();
                reason = MarkdownSanitizer.sanitize(reason);

                String type = document.getString("TYPE");

                if (type.equalsIgnoreCase("ROLE UPDATE")) type = "Role Update";
                type = type.substring(0, 1) + type.toLowerCase().substring(1);

                addToDescription = "\n**" + caseId + ":** " + type + ": *" + reason.trim() + "*";
            } else {
                long reportId = document.getLong("REPORT_ID");
                String reason = document.getString("REASON");

                addToDescription = "\n**" + reportId + ":** *" + reason.trim() + "*";
            }


            alert.appendDescription(addToDescription);
            count++;
        }
        if (count == 0) alert.appendDescription("\nN/A");
    }

}