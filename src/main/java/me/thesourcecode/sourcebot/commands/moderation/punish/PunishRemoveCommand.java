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
import java.util.HashMap;

import static me.thesourcecode.sourcebot.commands.moderation.punish.PunishCommand.getOffenseLevel;
import static me.thesourcecode.sourcebot.commands.moderation.punish.PunishCommand.getOffenses;

public class PunishRemoveCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "remove",
            "Removes the specified punishment",
            "<punishment id>",
            CommandInfo.Category.MODERATOR
    ).withControlRoles(SourceRole.STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        DatabaseManager dbManager = source.getDatabaseManager();

        // Invalid Id critical alert error message
        CriticalAlert invalidId = new CriticalAlert();
        invalidId.setTitle("Invalid Id!").setDescription("You did not enter a valid punishment id!");

        // Gets the id
        int punishId;
        try {
            punishId = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            return new MessageBuilder(invalidId.build(user)).build();
        }

        // Checks if the punishment exists
        HashMap<Integer, String> offenses = getOffenses(dbManager);
        if (offenses.get(punishId) == null) return new MessageBuilder(invalidId.build(user)).build();

        // Gets the punishment's name and level
        String removeOffense = offenses.get(punishId);
        int level = getOffenseLevel(punishId, dbManager);

        // Gets the mongo document for the offense list
        MongoCollection guildSettings = dbManager.getCollection("Settings");
        Document punishments = (Document) guildSettings.find(new Document("name", "offenses")).first();

        // Gets the level arraylist from the mongo document and removes the specified punishment
        ArrayList<String> punishmentLevel = (ArrayList<String>) punishments.get("level " + level);
        punishmentLevel.remove(removeOffense);

        // Updates the offense list mongo document
        punishments.append("level " + level, punishmentLevel);
        guildSettings.updateOne(new Document("name", "offenses"), new Document("$set", punishments));

        // Sends a success message
        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully removed the punishment: " + removeOffense);

        return new MessageBuilder(alert.build(message.getAuthor())).build();
    }

}
