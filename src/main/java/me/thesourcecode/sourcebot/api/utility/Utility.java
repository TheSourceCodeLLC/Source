package me.thesourcecode.sourcebot.api.utility;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import me.thesourcecode.sourcebot.commands.misc.coinflip.CoinflipCommand;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utility {

    public static final long vipLevel = 15;
    public static final long mvpLevel = 30;
    public static final ArrayList<String> urlBlacklist = new ArrayList<>();
    private static final Source source = Source.getInstance();
    private static final Pattern timeStrPattern = Pattern.compile("(\\d+)([yMdhms])");
    private static final HashMap<String, Integer> msgDeleteCache = new HashMap<>();

    /***
     *
     * @param timeString A time string, such as 24h30m30s.
     *                   y = years
     *                   M = months
     *                   d = days
     *                   h = hours
     *                   s = seconds
     * @return The calculated value of the time string, in milliseconds.
     */
    public static long millisFromTimeString(String timeString) {
        Matcher matcher = timeStrPattern.matcher(timeString);
        AtomicLong millis = new AtomicLong();
        matcher.results().forEach(result -> {
            int val = Integer.parseInt(result.group(1));
            String unit = result.group(2);
            switch (unit) {
                case "y":
                    millis.addAndGet(TimeUnit.DAYS.toMillis(val) * 365);
                    break;
                case "M":
                    millis.addAndGet(TimeUnit.DAYS.toMillis(val) * 30);
                    break;
                case "d":
                    millis.addAndGet(TimeUnit.DAYS.toMillis(val));
                    break;
                case "h":
                    millis.addAndGet(TimeUnit.HOURS.toMillis(val));
                    break;
                case "m":
                    millis.addAndGet(TimeUnit.SECONDS.toMillis(val));
                    break;
                case "s":
                    millis.addAndGet(TimeUnit.MILLISECONDS.toSeconds(val));
                    break;
                default:
                    millis.addAndGet(0);
            }
        });
        return millis.get();
    }

    public static Member getMemberByUsername(Guild guild, String username) {
        return guild.getMembers().stream().filter(m -> {
            String name = m.getUser().getAsTag();
            return username.equals(name);
        }).findFirst().orElse(null);
    }

    public static Member getMemberByIdentifier(Guild guild, String identifier) {
        boolean username = Pattern.matches(".+#\\d{4}", identifier);
        if (username) {
            return getMemberByUsername(guild, identifier);
        } else {
            identifier = identifier.replace("!", "");
            try {
                return guild.getMemberById(identifier.replaceAll("<@(\\d+)>", "$1"));
            } catch (NumberFormatException ex) {
                return null;
            }

        }
    }

    /**
     * Converts a duration type to it's full string (Ex: s -> Seconds)
     *
     * @param duration The durationType
     * @return the converted duration string
     */
    public static String convertDurationType(String duration) {
        switch (duration.replaceAll("[0-9]", "").toLowerCase()) {
            case "s":
                return "Seconds";
            case "m":
                return "Minutes";
            case "h":
                return "Hours";
            case "d":
                return "Days";
            case "w":
                return "Weeks";
            case "mo":
                return "Months";
        }
        return null;
    }

    /**
     * Gets the unpunish time in ms
     *
     * @param durationType The duration type
     * @param durationTime The amount of the duration type
     * @return The unpunish time in ms
     */
    public static long getUnpunishTime(String durationType, long durationTime) {
        switch (durationType.toLowerCase()) {
            case "second":
            case "seconds":
            case "s":
                return (durationTime * 1000) + System.currentTimeMillis();
            case "minute":
            case "minutes":
            case "m":
                return (durationTime * 60000) + System.currentTimeMillis();
            case "hour":
            case "hours":
            case "h":
                return (durationTime * 3600000) + System.currentTimeMillis();
            case "day":
            case "days":
            case "d":
                return (durationTime * 86400000) + System.currentTimeMillis();
            case "week":
            case "weeks":
            case "w":
                return (durationTime * 604800000) + System.currentTimeMillis();
            case "month":
            case "months":
            case "mo":
                return (durationTime * 2592000000L) + System.currentTimeMillis();
            default:
                return 0;
        }
    }

    /**
     * Gets the shop price for the specific id
     *
     * @param id The id of the item the user is buying
     * @return The cost of the item
     */
    public static int getShopPrices(int id) {
        switch (id) {
            case 1:
                return 200;
            case 2:
                return 250;
            case 3:
                return 400;
            default:
                return -1;
        }
    }


    public static String getCooldown(User user, String command) {
        SourceProfile profile = new SourceProfile(user);

        Document cooldowns = profile.getCooldowns();

        if (cooldowns.get(command) == null) return null;
        long expiration = cooldowns.getLong(command);

        if (expiration <= System.currentTimeMillis()) {
            cooldowns.remove(command);
            profile.setCooldowns(cooldowns);
            return null;
        }

        long remainingTime = expiration - System.currentTimeMillis();

        int seconds = (int) ((remainingTime / 1000) % 60);
        int minutes = (int) ((remainingTime / 1000) / 60);

        int hours = minutes / 60;
        minutes = hours != 0 ? minutes % 60 : minutes;
        long days = remainingTime / 86400000;

        hours = days != 0 ? hours % 24 : hours;
        String returnString = days != 0 ? days + (days == 1 ? " day" : " days") + "," : "";
        returnString += " " + (hours != 0 ? hours + (hours == 1 ? " hour" : " hours") + "," : "");
        returnString += " " + (minutes != 0 ? minutes + (minutes == 1 ? " minute" : " minutes") + "," : "");
        returnString += " " + (seconds != 0 ? seconds + (seconds == 1 ? " second" : " seconds") : "");

        if (returnString.endsWith(",")) {
            returnString = returnString.substring(1);
        }

        return returnString;
    }

    /**
     * Checks if there is a cooldown for the command being ran
     *
     * @param message The message object
     * @param command The command being ran
     * @param target  Whether or not to check for the user running the command or another user
     */
    public static boolean checkCooldown(Message message, String command, boolean target) {
        MessageChannel channel = message.getChannel();
        User user = message.getAuthor();

        String returnString = getCooldown(user, command);
        if (returnString == null || returnString.trim().isBlank() || returnString.equals(" ")) return false;

        CriticalAlert alert = new CriticalAlert();
        String aMsg = (target ? "The specified user" : "You") + " can not use that command for another " + returnString;
        alert.setDescription(aMsg);
        channel.sendMessage(alert.build(target ? channel.getJDA().getSelfUser() : user)).queue();
        return true;
    }

    /**
     * Checks if the message is a command
     *
     * @param message The message being checked
     * @return True or false depending whether or not the message was a command
     */
    public static boolean isCommand(Message message) {
        String rawContent = message.getContentRaw();
        if (rawContent.length() <= 1 && !rawContent.startsWith(CommandHandler.getPrefix())) {
            return false;
        }

        String[] args = rawContent.substring(1).split("\\s+");
        for (CommandInfo commandInfo : CommandHandler.getCommands().getCommandInfo()) {

            List<String> cmdAliases = new ArrayList<>(Arrays.asList(commandInfo.getAliases()));
            cmdAliases.add(commandInfo.getLabel());

            if (cmdAliases.contains(args[0].toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the xp required for the level inputted
     *
     * @param level The level that is 1 higher than the users
     * @return The amount of xp needed to rank up
     */
    public static long getXpFormula(long level) {

        return (9 * (level ^ 2) + 120 * level + 525); // Probs should be changed a little
    }

    /**
     * Gets all of the previous ranks xp and adds them together
     *
     * @param level The users level
     * @return The amount of xp all of the previous levels combined
     */
    public static long getAllPreviousLevels(long level) {
        long xp = 0;
        for (long i = 0; i < level; i++) {
            xp += getXpFormula(i + 1);
        }
        return xp;
    }

    public static String containsLink(Guild guild, String string) {
        Pattern pattern = Pattern.compile("(https?://)?(www.)?(discord\\.gg/)(\\w+)", Pattern.MULTILINE);

        String testMatch = string.replaceAll("\\s+", "\n").toLowerCase();
        Matcher matcher = pattern.matcher(testMatch);

        List<String> invites = new ArrayList<>();
        List<String> guildInvites = guild.retrieveInvites().complete()
                .stream().map(Invite::getCode).collect(Collectors.toList());

        List<String> fullInvite = new ArrayList<>();
        while (matcher.find()) {

            fullInvite.add(matcher.group(0));
            String inviteCode = matcher.group(0).replace(matcher.group(3), "");
            if (matcher.group(1) != null) {
                inviteCode = inviteCode.replace(matcher.group(1), "");
            }
            if (matcher.group(2) != null) {
                inviteCode = inviteCode.replace(matcher.group(2), "");
            }

            invites.add(inviteCode);
        }

        invites.removeIf(guildInvites::contains);
        if (invites.size() == 0) {
            return null;
        }

        int resolved;
        try {
            resolved = (int) invites.stream().map(code -> Invite.resolve(guild.getJDA(), code).complete()).filter(Objects::nonNull).count();
        } catch (Exception e) {
            return null;
        }
        if (resolved == 0) {
            return null;
        }

        for (String code : fullInvite) {
            string = string.replaceAll("(?i)" + code, "(Discord Invite)");
        }
        return string;
    }

    public static String escapeBold(String var) {
        return var.replace("**", "*\u200b*");
    }

    public static String formatList(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String string : list) {
            builder.append("`").append(string).append("` ");
        }
        return builder.toString().trim();
    }

    public static User getUserWhoDeletedMessage(Guild guild, User user) {
        AuditLogPaginationAction auditLog = guild.retrieveAuditLogs().type(ActionType.MESSAGE_DELETE);

        for (AuditLogEntry entry : auditLog.complete()) {

            if (entry.getTargetIdLong() != user.getIdLong()) continue;

            int deleteMsgCount = Integer.valueOf(entry.getOption(AuditLogOption.COUNT));
            if (msgDeleteCache.containsKey(user.getId())) {

                int oldMsgCount = msgDeleteCache.get(user.getId());

                if (deleteMsgCount == oldMsgCount) {
                    return user;
                } else {
                    msgDeleteCache.replace(user.getId(), oldMsgCount, deleteMsgCount);
                    return entry.getUser();
                }
            } else {
                msgDeleteCache.put(user.getId(), deleteMsgCount);

                String currentDate = formatAuditMillisCheck(System.currentTimeMillis());
                String deleteDate = formatAuditMillisCheck(entry.getTimeCreated().toInstant().toEpochMilli());

                if (currentDate.equalsIgnoreCase(deleteDate)) {
                    return entry.getUser();
                }
                break;
            }


        }
        return user;
    }

    private static String formatAuditMillisCheck(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat minute = new SimpleDateFormat("mm");
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, hh");

        String converted = minute.format(date);

        int minutes = Integer.parseInt(converted);
        minutes = Math.round(minutes / 5) * 5;

        return format.format(date) + ":" + minutes;
    }

    /**
     * Returns coins from currently cached coinflips
     */
    public static void safeStop() {
        HashMap<User, ArrayList<Object>> coinflip = CoinflipCommand.coinflip;
        if (coinflip.size() > 0) {
            coinflip.forEach((requester, info) -> {

                User target = (User) info.get(0);
                long coinAmount = (long) info.get(1);

                SourceProfile requesterProfile = new SourceProfile(requester);
                SourceProfile targetProfile = new SourceProfile(target);

                requesterProfile.addCoins(coinAmount);
                targetProfile.addCoins(coinAmount);
            });

            TextChannel botCommands = SourceChannel.COMMANDS.resolve(source.getJda());
            botCommands.sendMessage("All coinflips have been cancelled, due to an auto bot restart! Your coins have been refunded").queue();

            coinflip.clear();
        }

        SourceTag.saveAllTags();

    }

    public static double addPointsToUser(String userId, double pointsToAdd, long decayTime) {
        DatabaseManager dbManager = source.getDatabaseManager();
        MongoCollection<Document> userPunishments = dbManager.getCollection("Punishments");
        Document punishments = userPunishments.find(new Document("id", userId)).first();

        // Creates punishment document if it doesn't already exists
        if (punishments == null) {
            punishments = new Document("id", userId)
                    .append("points", 0D)
                    .append("decay", new ArrayList<String>());
        }

        double points = punishments.getDouble("points");
        ArrayList<String> decay = (ArrayList<String>) punishments.get("decay");

        points += pointsToAdd;
        if (points > 100) points = 100;

        // Keeps the decimal precision
        BigDecimal bd = new BigDecimal(Double.toString(points));
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        points = bd.doubleValue();

        // Appends the new point value
        punishments.append("points", points);

        // Appends the new decay value unless the point count is 100
        if (points != 100) {
            decay.add(77.8 + " " + decayTime);
            punishments.append("decay", decay);
        }

        // Finds and creates/updates the punishment document for the target
        Document find = userPunishments.find(new Document("id", userId)).first();
        if (find == null) {
            userPunishments.insertOne(punishments);
        } else {
            Document query = new Document("id", userId);
            userPunishments.updateOne(query, new Document("$set", punishments));
        }
        return points;
    }

}

