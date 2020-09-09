package me.thesourcecode.sourcebot.commands.developer.blacklist;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceBlacklist;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BlacklistDevelopmentCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "blacklist",
            "Blacklists the specified user from the dev channels.",
            "(add|remove|list) <@user|id|name> <id>",
            CommandInfo.Category.DEVELOPER)
            .withControlRoles(SourceRole.DEVELOPERS_STAFF)
            .withAliases("bl");

    public BlacklistDevelopmentCommand() {
        registerSubcommand(new DevelopmentAddCommand());
        registerSubcommand(new DevelopmentRemoveCommand());
        registerSubcommand(new DevelopmentListCommand());
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

        CommonAlerts commonAlerts = new CommonAlerts();

        HashMap<Integer, String> punishmentList = getPunishments(source.getDatabaseManager());
        // Checks if the target is non existent
        if (target == null) {
            return new MessageBuilder(commonAlerts.invalidUser(user)).build();
        }

        // Checks if the target is a bot/fake/themselves
        MessageEmbed invalidUser = commonAlerts.invalidBlacklistUser(user, target, "development help channels");

        if (invalidUser != null) {
            return new MessageBuilder(invalidUser).build();
        }

        User targetUser = target.getUser();
        int id;

        CriticalAlert invalidPunishmentId = new CriticalAlert();
        invalidPunishmentId.setTitle("Uh Oh!").setDescription("You did not enter a valid punishment id!");
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            return new MessageBuilder(invalidPunishmentId.build(user)).build();
        }

        if (punishmentList.get(id) == null) {
            return new MessageBuilder(invalidPunishmentId.build(user)).build();
        }

        String[] punishmentArgs = punishmentList.get(id).split("\\s+");
        String pDuration = punishmentArgs[0];
        String reason = String.join(" ", Arrays.copyOfRange(punishmentArgs, 1, punishmentArgs.length));

        int duration = Integer.parseInt(pDuration.replaceAll("[^\\d.]", ""));
        String durationType = pDuration.replaceAll("[0-9]", "");

        // Converts the type to the full type (Ex: s -> Seconds)
        String fullType = Utility.convertDurationType(durationType);

        // Combines the duration with the full type (Ex: 2 Seconds);
        String durationString = duration + " " + (duration == 1 ? fullType.substring(0, fullType.length() - 1) : fullType);

        SourceBlacklist sourceBlacklist = new SourceBlacklist(user.getId(), targetUser.getId(), durationString, reason);
        if (sourceBlacklist.execute()) {
            sourceBlacklist.sendIncidentEmbed();

            // Sends a success message
            SuccessAlert sAlert = new SuccessAlert();
            sAlert.setDescription("You have successfully blacklisted " + targetUser.getAsTag() + "!");

            return new MessageBuilder(sAlert.build(user)).build();
        }
        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setTitle("Error!").setDescription("I could not blacklist that user!");
        return new MessageBuilder(cAlert.build(user)).build();
    }

    private HashMap<Integer, String> getPunishments(DatabaseManager dbManager) {
        MongoCollection settings = dbManager.getCollection("Settings");
        Document devBlacklist = (Document) settings.find(new Document("name", "devBlacklist")).first();

        if (devBlacklist == null) {
            ArrayList<String> punishments = new ArrayList<>(Arrays.asList("15m Off topic in development help channels", "15m Not using a bin",
                    "15m Please take this time to learn the basics of the language before asking for help", "1h Seeking assistance with malicious intent"));
            devBlacklist = new Document("name", "devBlacklist")
                    .append("punishments", punishments);
            settings.insertOne(devBlacklist);
        }

        ArrayList<String> punishments = (ArrayList<String>) devBlacklist.get("punishments");
        HashMap<Integer, String> punishmentList = new HashMap<>(); // ID, Duration + Reason

        punishments.forEach(punishment ->
                punishmentList.put(punishmentList.size() + 1, punishment)
        );

        return punishmentList;
    }

    private void updatePunishments(DatabaseManager dbManager, HashMap<Integer, String> newPunishList) {
        MongoCollection settings = dbManager.getCollection("Settings");
        Document devBlacklist = (Document) settings.find(new Document("name", "devBlacklist")).first();

        ArrayList<String> punishmentList = new ArrayList<>(newPunishList.values());
        devBlacklist.append("punishments", punishmentList);

        settings.updateOne(new Document("name", "devBlacklist"), new Document("$set", devBlacklist));
    }

    private class DevelopmentAddCommand extends Command {
        private final CommandInfo INFO = new CommandInfo(
                "add",
                "Adds a punishment to the development blacklist punishment list",
                "<duration + s|m|h> <reason>",
                CommandInfo.Category.DEVELOPER)
                .withControlRoles(SourceRole.DEV_LEAD, SourceRole.DEV, SourceRole.ADMIN, SourceRole.OWNER, SourceRole.MODERATOR)
                .withAliases("create");

        @Override
        public CommandInfo getInfo() {
            return INFO;
        }

        @Override
        public Message execute(Source source, Message message, String[] args) {
            User user = message.getAuthor();

            int duration;
            String type;

            String[] types = {"s", "m", "h"};

            CommonAlerts cAlerts = new CommonAlerts();
            try {
                duration = Integer.parseInt(args[0].replaceAll("[^\\d.]", ""));
                type = args[0].replaceAll("[0-9]", "").toLowerCase();
            } catch (NumberFormatException ex) {
                return new MessageBuilder(cAlerts.invalidDuration(user, "")).build();
            }

            // Checks if duration is less than or equal to 0 and checks if the duration type is valid
            if (duration <= 0) {
                return new MessageBuilder(cAlerts.invalidDuration(user, "time")).build();
            } else if (!Arrays.asList(types).contains(type)) {
                return new MessageBuilder(cAlerts.invalidDuration(user, "type")).build();
            }

            long durationInMs = Utility.getUnpunishTime(type, duration) - System.currentTimeMillis();
            long max = 10800000; // 3 Hours

            if (durationInMs > max) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("You can not add a development blacklist punishment that is longer than 3 hours!");
                return new MessageBuilder(alert.build(user)).build();
            }

            String durationString = duration + type.toLowerCase();
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            HashMap<Integer, String> punishments = getPunishments(source.getDatabaseManager());
            punishments.put(punishments.size() + 1, durationString + " " + reason);
            updatePunishments(source.getDatabaseManager(), punishments);

            SuccessAlert alert = new SuccessAlert();
            alert.setDescription("You have successfully added a punishment to the development blacklist punishment list!");
            return new MessageBuilder(alert.build(user)).build();

        }
    }

    private class DevelopmentRemoveCommand extends Command {
        private final CommandInfo INFO = new CommandInfo(
                "remove",
                "Removes a punishment from the development blacklist punishment list",
                "<id>",
                CommandInfo.Category.DEVELOPER)
                .withControlRoles(SourceRole.DEV_LEAD, SourceRole.DEV, SourceRole.ADMIN, SourceRole.OWNER, SourceRole.MODERATOR)
                .withAliases("delete");

        @Override
        public CommandInfo getInfo() {
            return INFO;
        }

        @Override
        public Message execute(Source source, Message message, String[] args) {
            User user = message.getAuthor();

            int id;

            CriticalAlert invalidId = new CriticalAlert();
            invalidId.setTitle("Uh Oh!").setDescription("You did not enter a valid punishment id!");
            try {
                id = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                return new MessageBuilder(invalidId.build(user)).build();
            }

            HashMap<Integer, String> punishments = getPunishments(source.getDatabaseManager());

            if (punishments.get(id) == null) {
                return new MessageBuilder(invalidId.build(user)).build();
            }

            punishments.remove(id);
            updatePunishments(source.getDatabaseManager(), punishments);

            SuccessAlert alert = new SuccessAlert();
            alert.setDescription("You have successfully removed a punishment from the development blacklist punishment list!");
            return new MessageBuilder(alert.build(user)).build();

        }
    }

    private class DevelopmentListCommand extends Command {
        private final CommandInfo INFO = new CommandInfo(
                "list",
                "Sends the user the development blacklist punishment list")
                .withControlRoles(SourceRole.DEV_LEAD, SourceRole.DEV, SourceRole.ADMIN, SourceRole.OWNER, SourceRole.MODERATOR);

        @Override
        public CommandInfo getInfo() {
            return INFO;
        }

        @Override
        public Message execute(Source source, Message message, String[] args) {
            User user = message.getAuthor();
            HashMap<Integer, String> punishmentList = getPunishments(source.getDatabaseManager());

            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
            punishmentList.forEach((id, punishment) -> {
                String[] punishmentArgs = punishment.split("\\s+");
                String pDuration = punishmentArgs[0];
                String reason = String.join(" ", Arrays.copyOfRange(punishmentArgs, 1, punishmentArgs.length))
                        .replace("`", "\\`");

                int duration = Integer.parseInt(pDuration.replaceAll("[^\\d.]", ""));
                String durationType = pDuration.replaceAll("[0-9]", "");

                // Converts the type to the full type (Ex: s -> Seconds)
                String fullType = Utility.convertDurationType(durationType);

                // Combines the duration with the full type (Ex: 2 Seconds);
                String durationString = duration + " " + (duration == 1 ? fullType.substring(0, fullType.length() - 1) : fullType);

                String format = "**%d.** `%s - %s`\n";
                alert.appendDescription(String.format(format, id, durationString, reason));

            });

            message.getChannel().sendMessage(alert.build(user, "Development Mute Offense List")).queue();
            return null;
        }
    }
}
