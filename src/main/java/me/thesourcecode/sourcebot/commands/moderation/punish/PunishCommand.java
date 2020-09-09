package me.thesourcecode.sourcebot.commands.moderation.punish;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.*;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PunishCommand extends Command {
    private final NavigableMap<Double, String> punishments = new TreeMap<>() {{
        put(3.70, "Warn");
        put(7.40, "Warn"); // Staff: warning
        put(11.10, "15m Tempmute");
        put(14.80, "30m Tempmute");
        put(18.50, "1h Tempmute");
        put(22.20, "3h Tempmute"); // Staff: 3 day role removal
        put(25.90, "5h Tempmute");
        put(29.60, "7h Tempmute");
        put(33.30, "1d Tempmute"); // Staff: perm role removal
        put(37.00, "2d Tempmute");
        put(40.70, "3d Tempmute");
        put(44.40, "4d Tempmute");
        put(48.10, "5d Tempmute");
        put(51.80, "6d Tempmute");
        put(55.50, "1w Tempmute");
        put(59.20, "2w Tempmute");
        put(62.90, "3w Tempmute"); // That would suck being muted for 3 weeks i would just leave
        put(66.60, "2d Tempban");
        put(70.30, "4d Tempban");
        put(74.00, "6d Tempban");
        put(77.80, "1w Tempban");
        put(81.40, "3w Tempban");
        put(85.10, "1mo Tempban");
        put(88.80, "2mo Tempban");
        put(92.50, "3mo Tempban");
        put(96.30, "4mo Tempban");
        put(100.00, "Ban"); // Man you actually had to try to do this one
    }};

    private final CommandInfo INFO = new CommandInfo(
            "punish",
            "Punishes the specified user.",
            "(list|add|remove) <@user|id|name> <punishment id>",
            CommandInfo.Category.MODERATOR
    ).withControlRoles(SourceRole.STAFF_MOD);

    public PunishCommand() {
        registerSubcommand(new PunishListCommand());
        registerSubcommand(new PunishAddCommand());
        registerSubcommand(new PunishRemoveCommand());
    }

    /**
     * Gets the offense list from mongo
     *
     * @param dbManager The database manager
     * @return The offense hashmap
     */
    static HashMap<Integer, String> getOffenses(DatabaseManager dbManager) {
        HashMap<Integer, String> offenses = new HashMap<>();

        MongoCollection guildSettings = dbManager.getCollection("Settings");
        Document punishmentsDocument = (Document) guildSettings.find(new Document("name", "offenses")).first();

        int count = 1;
        for (int level = 1; level <= 5; level++) {
            ArrayList<String> foundOffenses = (ArrayList<String>) punishmentsDocument.get("level " + level);

            for (String offenseName : foundOffenses) {
                offenses.put(count, offenseName);
                count++;
            }
        }
        return offenses;

    }

    /**
     * Gets the specified offense's level
     *
     * @param id        The id of the offense
     * @param dbManager The database manager
     * @return The level of the offense
     */
    static int getOffenseLevel(int id, DatabaseManager dbManager) {
        MongoCollection guildSettings = dbManager.getCollection("Settings");
        Document punishmentsDocument = (Document) guildSettings.find(new Document("name", "offenses")).first();

        int count = 1;
        for (int level = 1; level <= 5; level++) {
            ArrayList<String> foundOffenses = (ArrayList<String>) punishmentsDocument.get("level " + level);

            for (String ignored : foundOffenses) {
                if (count == id) {
                    return level;
                }
                count++;
            }
        }
        return 0;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {

        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);
        CommonAlerts alert = new CommonAlerts();

        // Checks if the target is non existent
        if (target == null) {
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();

        // Checks if the target is a bot/fake/themselves
        MessageEmbed invalidUser = alert.invalidUser(user, target, "punish");
        if (invalidUser != null) return new MessageBuilder(invalidUser).build();

        CriticalAlert invalidId = new CriticalAlert();
        invalidId.setTitle("Invalid Id!").setDescription("You did not enter a valid punishment id!");

        // Gets the punishment id
        int punishmentId;
        try {
            punishmentId = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            return new MessageBuilder(invalidId.build(user)).build();
        }
        if (getOffenses(source.getDatabaseManager()) == null) {
            CriticalAlert cAlert = new CriticalAlert();
            cAlert.setTitle("Uh Oh!").setDescription("It appears that there are no defined punishments! To add some to `!punish add <level 1-5> <punishment name>`");
            return new MessageBuilder(cAlert.build(user)).build();
        }

        if (getOffenses(source.getDatabaseManager()).get(punishmentId) == null) {
            return new MessageBuilder(invalidId.build(user)).build();
        }

        double addPoints = 0;
        long decayTime = 2592000000L; // One month
        int offenseLevel = getOffenseLevel(punishmentId, source.getDatabaseManager());

        // Gets the points and decay time based on the offense level
        switch (offenseLevel) {
            case 1:
                addPoints = 3.70;
                decayTime += System.currentTimeMillis();
                break;
            case 2:
                addPoints = 11.11;
                decayTime = (decayTime * 3) + System.currentTimeMillis();
                break;
            case 3:
                addPoints = 33.33;
                decayTime = (decayTime * 6) + System.currentTimeMillis();
                break;
            case 4:
                addPoints = 77.78;
                decayTime = (decayTime * 8) + System.currentTimeMillis();
                break;
            case 5:
                addPoints = 100.00;
                break;
        }

        double points = Utility.addPointsToUser(targetUser.getId(), addPoints, decayTime);

        String punishmentGiven = punishments.ceilingEntry(points).getValue();
        String type = punishmentGiven;
        String reason = getOffenses(source.getDatabaseManager()).get(punishmentId).trim();
        String incidentReason = reason + " (Punishments vary based on your punishment history)";

        String durationType;
        int durationTime;
        String converted;
        String durationString = "";

        // Gets the duration length for tempbans and tempmutes
        if (punishmentGiven.contains("Tempban") || punishmentGiven.contains("Tempmute")) {
            String[] punishArgs = punishmentGiven.split(" ");
            type = punishArgs[1];

            durationType = punishArgs[0].replaceAll("[0-9]", "");
            durationTime = Integer.parseInt(punishArgs[0].replaceAll("[a-z]", ""));

            converted = Utility.convertDurationType(durationType);
            durationString = durationTime + " " + (durationTime == 1 ? converted.substring(0, converted.length() - 1) : converted);
        }

        SourceIncident sourceIncident;
        switch (type.toLowerCase()) {
            case "tempmute":
                sourceIncident = new SourceMute(user.getId(), targetUser.getId(), durationString, incidentReason);
                break;
            case "ban":
                sourceIncident = new SourceBan(user.getId(), targetUser.getId(), incidentReason);
                break;
            case "tempban":
                sourceIncident = new SourceTempban(user.getId(), targetUser.getId(), durationString, incidentReason);
                break;
            case "kick":
                sourceIncident = new SourceKick(user.getId(), targetUser.getId(), incidentReason);
                break;
            case "warn":
                sourceIncident = new SourceWarn(user.getId(), targetUser.getId(), incidentReason);
                break;
            default:
                return new MessageBuilder("Uh Oh! Something went wrong, please notify TheForbiddenAi.").build();
        }

        if (sourceIncident.execute()) {
            sourceIncident.sendIncidentEmbed();

            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully punished " + targetUser.getAsTag() + " for " + reason + "!");

            return new MessageBuilder(sAlert.build(user)).build();
        }
        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setTitle("Error").setDescription("I could not punish that user!");
        return new MessageBuilder(cAlert.build(user)).build();
    }
}
