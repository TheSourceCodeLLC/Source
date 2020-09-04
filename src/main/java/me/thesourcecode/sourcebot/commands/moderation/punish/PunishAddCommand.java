package me.thesourcecode.sourcebot.commands.moderation.punish;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;

public class PunishAddCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "add",
            "Adds a punishment to the punishment list",
            "<level 1-5> <punishment name>",
            CommandInfo.Category.MODERATOR
    ).withControlRoles(SourceRole.STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        // Gets the level and name of the new punishment
        int level;
        String punishName = String.join(" ", args).substring(args[0].length() + 1).trim();
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            level = 0;
        }

        // Checks if the punishment is less than 1 or greater than 5
        if (level < 1 || level > 5) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid punishment level!");
            return new MessageBuilder(alert.build(user)).build();
        }

        try {
            DatabaseManager dbManager = source.getDatabaseManager();
            MongoCollection guildSettings = dbManager.getCollection("Settings");

            // Gets the mongo document for the offense list
            Document punishments = (Document) guildSettings.find(new Document("name", "offenses")).first();

            // Checks if the document exists, if not it will create one
            if (punishments == null) {
                punishments = new Document("name", "offenseList")
                        .append("level 1", new ArrayList<String>())
                        .append("level 2", new ArrayList<String>())
                        .append("level 3", new ArrayList<String>())
                        .append("level 4", new ArrayList<String>())
                        .append("level 5", new ArrayList<String>());
                guildSettings.insertOne(punishments);
            }

            // Gets the level arraylist from the mongo document and adds the new punishment
            ArrayList<String> punishmentLevel = (ArrayList<String>) punishments.get("level " + level);
            punishmentLevel.add(punishName);

            // Updates the offense list mongo document
            punishments.append("level " + level, punishmentLevel);
            guildSettings.updateOne(new Document("name", "offenses"), new Document("$set", punishments));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Sends a success message
        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully added the punishment: " + punishName + ", to punishment level: " + level);

        return new MessageBuilder(alert.build(message.getAuthor())).build();
    }
}
