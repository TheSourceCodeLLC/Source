package me.thesourcecode.sourcebot.commands.developer.blacklist;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;

public class BlacklistStatusCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "status",
            "Adds or removes a url to the status url blacklist.",
            "<add|remove|list> (url)",
            CommandInfo.Category.DEVELOPER)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER, SourceRole.MODERATOR);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        DatabaseManager dbManager = source.getDatabaseManager();


        MongoCollection<Document> settings = dbManager.getCollection("Settings");
        Document urlBlackListDocument = settings.find(new Document("name", "urlBlacklist")).first();
        if (urlBlackListDocument == null) {
            urlBlackListDocument = new Document("name", "urlBlacklist")
                    .append("urls", new ArrayList<String>());

            settings.insertOne(urlBlackListDocument);
        }
        dbManager.urlBlacklist = (ArrayList<String>) urlBlackListDocument.get("urls");

        CriticalAlert alert = new CriticalAlert();
        SuccessAlert sAlert = new SuccessAlert();

        String url = null;
        if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
            if (args.length == 1) {
                alert.setTitle("Invalid Usage!")
                        .setDescription("Syntax: " + CommandHandler.getPrefix() + "blacklist " + INFO.getLabel() + " " + INFO.getArguments());
                return new MessageBuilder(alert.build(message.getAuthor())).build();
            }
            url = args[1].toLowerCase();
        }
        switch (args[0].toLowerCase()) {
            case "list":
                ColoredAlert list = new ColoredAlert(SourceColor.BLUE);
                StringBuilder listBuilder = new StringBuilder();
                if (dbManager.urlBlacklist.size() == 0) {
                    list.setDescription("There are currently no blacklisted urls from the member status check!");
                } else {
                    dbManager.urlBlacklist.forEach(blacklistedUrl -> listBuilder.append("`").append(blacklistedUrl).append("` "));

                    list.setDescription("Blacklisted urls from the member status check:\n" + listBuilder.toString().trim());
                }
                message.getChannel().sendMessage(list.build(user)).queue();
                return null;
            case "add":
                if (dbManager.urlBlacklist.contains(url)) {
                    alert.setTitle("Uh Oh!").setDescription("That url is already blacklisted from the member status check!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                sAlert.setDescription("You have successfully added a url to the member status check blacklist!");
                dbManager.urlBlacklist.add(url);
                break;
            case "remove":
                if (!dbManager.urlBlacklist.contains(url)) {
                    alert.setTitle("Uh Oh!").setDescription("That url is already not blacklisted from the member status check!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                sAlert.setDescription("You have successfully removed a url from the member status check blacklist!");
                dbManager.urlBlacklist.remove(url);
                break;
            default:
                alert.setTitle("Uh Oh!").setDescription("You did not specify whether you wanted to add or a remove a url from the blacklist!");
                return new MessageBuilder(alert.build(user)).build();
        }

        urlBlackListDocument.put("urls", dbManager.urlBlacklist);

        ArrayList<String> urlBlacklist = Utility.urlBlacklist;
        urlBlacklist.clear();
        urlBlacklist.addAll((ArrayList<String>) urlBlackListDocument.get("urls"));

        settings.updateOne(new Document("name", "urlBlacklist"), new Document("$set", urlBlackListDocument));
        return new MessageBuilder(sAlert.build(user)).build();
    }
}
