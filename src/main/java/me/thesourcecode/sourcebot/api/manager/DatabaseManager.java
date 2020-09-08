package me.thesourcecode.sourcebot.api.manager;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.thesourcecode.sourcebot.BotMain;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.Listener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import me.thesourcecode.sourcebot.listener.CapsListener;
import me.thesourcecode.sourcebot.listener.CodeBlockListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {
    private final ScheduledExecutorService executorService;

    private final Pattern pingPattern = Pattern.compile("<@(\\d+)>", Pattern.MULTILINE);
    private final DateTimeFormatter
            dateFormat = BotMain.DATE_FORMAT,
            timeFormat = BotMain.TIME_FORMAT;
    private final Listener listener;
    private final Guild guild;
    private final MongoDatabase database;
    private final MongoCollection<Document> log, userdata, recovery, verify;
    private final boolean beta;
    public ArrayList<String> urlBlacklist = new ArrayList<>();

    public DatabaseManager(Source source, JSONObject config) {
        this.beta = source.isBeta();
        this.executorService = source.getExecutorService();
        this.listener = source.getListener();
        JDA jda = source.getJda();
        this.guild = (beta ? SourceGuild.BETA : SourceGuild.MAIN).resolve(jda);

        String username = URLEncoder.encode(config.getString(beta ? "username" : "musername"), StandardCharsets.UTF_8);
        String password = URLEncoder.encode(config.getString(beta ? "password" : "mpassword"), StandardCharsets.UTF_8);

        String host = config.getString("host");
        int port = config.getInt("port");


        String url = "mongodb://" + username + ":" + password + "@" + host + (beta ? "" : ":" + port) + "/admin?authSource=admin";

        MongoClientURI uri = new MongoClientURI(url);
        MongoClient client = new MongoClient(uri);

        String dbString = beta ? "beta-sourcebot" : "sourcebot";
        this.database = client.getDatabase(dbString);

        this.log = getCollection("MessageLog");
        this.userdata = getCollection("UserData");
        this.recovery = getCollection("Recovery");
        this.verify = getCollection("Verify");

        deleteOldData();
        checkVerify();
        listenEvents();
        checkLogs();
        checkGiveaways(source);
        //autoRestart(jda);
    }

    private void autoRestart(JDA jda) {
        executorService.schedule(() -> {
            Utility.safeStop();

            try {
                String jarname = beta ? "sourcebeta" : "sourcebot";

                Runtime.getRuntime().exec("systemctl --user restart " + jarname);
            } catch (Exception ex) {
                ex.printStackTrace();
                TextChannel adminChat = SourceChannel.ADMINS.resolve(jda);
                adminChat.sendMessage("Failed to auto restart!").queue();
            }
        }, 12, TimeUnit.HOURS);
    }

    private void checkGiveaways(Source source) {
        MongoCollection mongoGiveaways = getCollection("Giveaways");

        executorService.scheduleAtFixedRate(() -> {
            try {
                if (mongoGiveaways != null && mongoGiveaways.countDocuments() != 0) {
                    for (Object object : mongoGiveaways.find()) {
                        Document found = (Document) object;
                        long expires = found.getLong("END");
                        boolean expired = found.getBoolean("EXPIRED");
                        if (expires <= System.currentTimeMillis() && !expired) {
                            long id = found.getLong("ID");

                            Document update = new Document("EXPIRED", true);
                            Document query = new Document().append("ID", id);
                            mongoGiveaways.updateOne(query, new Document("$set", update));

                            TextChannel giveawayChannel = source.getJda().getTextChannelById(found.getString("CHANNEL_ID"));
                            String reward = found.getString("REWARD");
                            String senderId = found.getString("USER_ID");
                            long winner_ammount = found.getLong("WINNER_AMOUNT");

                            Message gaMessage;
                            try {
                                gaMessage = giveawayChannel.retrieveMessageById(found.getString("MESSAGE_ID")).complete();
                            } catch (Exception ex) {
                                mongoGiveaways.deleteOne(found);
                                continue;
                            }


                            EmbedBuilder newEmbed = new EmbedBuilder()
                                    .setAuthor(reward + " | Id: " + id, null, source.getJda().getSelfUser().getAvatarUrl())
                                    .setColor(SourceColor.RED.asColor())
                                    .setDescription("**Expires In:** Expired" +
                                            "\n**Amount of Winners:** " + winner_ammount)
                                    .setFooter(gaMessage.getEmbeds().get(0).getFooter().getText().replace("Expires On:", "Expired On:"), null);
                            gaMessage.editMessage(newEmbed.build()).queue();

                            List<User> users = gaMessage.getReactions().get(0).retrieveUsers().complete();

                            Iterator it = users.iterator();
                            while (it.hasNext()) {
                                User user = (User) it.next();
                                if (user.isBot() || user.getId().equals(senderId)) it.remove();
                            }

                            List<User> winners = new ArrayList<>();
                            boolean skip = false;
                            if (users.size() <= winner_ammount) {
                                if (users.size() == 0) {
                                    EmbedBuilder embed = new EmbedBuilder()
                                            .setAuthor(reward, null, source.getJda().getSelfUser().getAvatarUrl())
                                            .setColor(SourceColor.RED.asColor())
                                            .setDescription("No one won the reward `" + reward + "`!");

                                    giveawayChannel.sendMessage(embed.build()).queue();
                                    return;
                                }
                                winners.addAll(users);
                                skip = true;
                            }
                            if (!skip) {
                                int max = users.size() - 1;

                                Random rand = new Random();
                                int randomNum = rand.nextInt((max - 1) + 1) + 1;
                                if (winner_ammount > 1) {
                                    for (int i = 0; i < winner_ammount; i++) {
                                        randomNum = rand.nextInt((max - 1) + 1) + 1;
                                        User uFound = users.get(randomNum);

                                        if (winners.contains(uFound) || uFound.isBot()) {
                                            continue;
                                        }
                                        winners.add(uFound);
                                    }
                                } else {
                                    winners.add(users.get(randomNum));
                                }
                            }

                            reward = reward.replace("`", "").trim();
                            String winnerMessage = "__%s | Id: %d__\n\nThe %s %s %s won `%s`";

                            StringBuilder sbWinners = new StringBuilder();
                            winners.forEach(winnerUser -> {
                                sbWinners.append(winnerUser.getAsMention()).append(", ");
                            });

                            boolean singularWinner = winner_ammount == 1;

                            sbWinners.deleteCharAt(sbWinners.length() - 2);

                            if (!singularWinner) {
                                int lastIndexOfComma = sbWinners.lastIndexOf(",");
                                sbWinners.replace(lastIndexOfComma, lastIndexOfComma + 1, " and");
                            }


                            String winnersList = sbWinners.toString().trim();


                            winnerMessage = String.format(winnerMessage, reward, id, singularWinner ? "user" : "users", winnersList,
                                    singularWinner ? "has" : "have", reward);

                            giveawayChannel.sendMessage(winnerMessage).queue();

                        }


                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);

    }

    private void deleteOldData() {
        executorService.scheduleAtFixedRate(() -> {
            for (Object o : recovery.find()) {
                Document data = (Document) o;
                long end = data.getLong("erase");
                long now = System.currentTimeMillis();
                if (end < now) {
                    recovery.deleteOne(data);
                }
            }
        }, 0L, 15, TimeUnit.MINUTES);
    }

    private void checkVerify() {
        executorService.scheduleAtFixedRate(() -> {
            for (Object object : verify.find()) {
                Document document = (Document) object;
                long expiration = document.getLong("EXPIRE");
                if (expiration <= System.currentTimeMillis()) {
                    String id = document.getString("ID");
                    Member member = guild.getMemberById(id);

                    try {
                        PrivateChannel channel = member.getUser().openPrivateChannel().complete();
                        channel.sendMessage("Your captcha has expired! Because of this you have been kicked!").complete();
                    } catch (Exception ignored) {
                    }
                    guild.kick(member, "Did not enter captcha in time.").queue();
                    verify.deleteOne(document);
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void listenEvents() {
        listener.handle(GuildMessageReceivedEvent.class, this::logMessage)
                .handle(GuildMessageDeleteEvent.class, this::deleteMessage)
                .handle(GuildMessageUpdateEvent.class, this::editMessage)
                .handle(GuildMemberJoinEvent.class, this::processMemberJoin)
                .handle(GuildMemberLeaveEvent.class, this::processUserLeave);
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    private void checkLogs() {
        final long expire = 86400000 * 7; // 1 week
        executorService.scheduleAtFixedRate(() -> {
            for (Object object : log.find()) {

                Document document = (Document) object;
                long timeMade = document.getLong("currentTime");
                if (System.currentTimeMillis() >= (timeMade + expire)) {
                    log.deleteOne(document);
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private void logMessage(GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String prefix = CommandHandler.getPrefix();

        User user = message.getAuthor();
        if (user.isBot() || user.equals(message.getJDA().getSelfUser())) {
            return;
        }

        if (content.startsWith(prefix)) {
            String testLabel = content.replaceFirst(prefix, "").split(" ")[0].toLowerCase();
            Command match = CommandHandler.getCommands().getCommand(testLabel);
            if (match != null) {
                return;
            }
        }

        TextChannel channel = event.getChannel();
        if (channel == SourceChannel.ADMINS.resolve(event.getJDA())
                || channel == SourceChannel.APPLICATIONS.resolve(event.getJDA())) {
            return;
        }

        long messageId = message.getIdLong();

        long userId = user.getIdLong();

        ZonedDateTime date = ZonedDateTime.now(BotMain.TIME_ZONE);

        Document document = new Document()
                .append("id", messageId)
                .append("author", String.format("%#s", user))
                .append("authorId", userId)
                .append("message", content);
        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.size() >= 1) {
            document.append("attachments", attachments.get(0).getProxyUrl());
        }
        document.append("date", dateFormat.format(date))
                .append("time", timeFormat.format(date))
                .append("currentTime", System.currentTimeMillis());

        log.insertOne(document);
    }

    private void editMessage(GuildMessageUpdateEvent event) {
        Message message = event.getMessage();
        TextChannel channel = message.getTextChannel();
        JDA jda = message.getJDA();
        TextChannel logChannel = SourceChannel.MESSAGE_LOG.resolve(jda);
        long messageId = message.getIdLong();
        Document record = log.find(new Document("id", messageId)).first();
        if (record == null) {
            return;
        }
        String newContent = message.getContentRaw();
        String authorName = record.getString("author");
        long authorId = record.getLong("authorId");
        Member member = message.getGuild().getMemberById(authorId);
        User author;
        if (member != null) {
            author = member.getUser();
        } else {
            author = jda.getSelfUser();
        }

        String authorFormat = "%s (%d)";
        authorFormat = String.format(authorFormat, authorName, authorId);

        String oldMessage = record.getString("message");
        oldMessage = oldMessage.isEmpty() ? "N/A" : oldMessage;

        String dateTime = "%s at %s";
        dateTime = String.format(dateTime, record.getString("date"), record.getString("time"));

        String channelFormat = "%s (%d)";
        channelFormat = String.format(channelFormat, channel.getName(), channel.getIdLong());


        String format = "" +
                "**Author:** %s\n" +
                "**Message:** ```diff\n%s\n\n%s\n```\n" +
                "**Channel:** %s\n" +
                "**Message Sent At:** %s";
        StringBuilder newDiff = new StringBuilder();
        StringBuilder oldDiff = new StringBuilder();

        String[] newLines = newContent.split("\n");
        String[] oldLines = oldMessage.split("\n");

        for (String newLine : newLines) {
            newDiff.append("+ ").append(newLine).append("\n");
        }

        for (String oldLine : oldLines) {
            oldDiff.append("- ").append(oldLine).append("\n");
        }

        try {
            WarningAlert alert = new WarningAlert();
            alert.setTitle("Message Edited").setDescription(String.format(format, authorFormat, newDiff.toString().trim().replace("```", "`\u200b``")
                    , oldDiff.toString().trim().replace("```", "`\u200b``"), channelFormat, dateTime));

            if (record.getString("attachment") != null) {
                alert.setThumbnail(record.getString("attachment"));
            }

            logChannel.sendMessage(alert.build(author)).queue();
        } catch (Exception ignored) {
            log.deleteOne(record);
        }

        Bson updated = new Document("$set", new Document("message", newContent));
        log.updateOne(record, updated);


        if (!SourceRole.ignoresModeration(member)) {
            newContent = Utility.containsLink(guild, newContent) != null ? Utility.containsLink(guild, newContent) : newContent;
        }
        oldMessage = oldMessage.replaceAll("<@&(\\d+)>", "<@$1>").replaceAll("<@!(\\d+)>", "<@$1>");
        newContent = newContent.replaceAll("<@&(\\d+)>", "<@$1>").replaceAll("<@!(\\d+)>", "<@$1>");
        oldMessage = oldMessage.replace("\n", "");

        Matcher codeMatcher = CodeBlockListener.pattern.matcher(newContent);
        ArrayList<TextChannel> ignoreGhost = new ArrayList<>();

        Arrays.stream(SourceChannel.ignoreGhost).forEach(sourceChannel -> {
            ignoreGhost.add(sourceChannel.resolve(event.getJDA()));
        });


        if (!CapsListener.checkCaps(message.getContentRaw(), null) && !codeMatcher.find() &&
                !ignoreGhost.contains(channel)) {

            if (SourceRole.ignoresModeration(member)) return;

            Matcher oldMatcher = pingPattern.matcher(oldMessage.replaceAll("\\s+", "\n").trim());

            StringBuilder findOldRoles = new StringBuilder();
            while (oldMatcher.find()) {
                String role = oldMatcher.group(0);
                if (!findOldRoles.toString().contains(role)) findOldRoles.append(role);
            }

            String[] oldRoles = findOldRoles.toString().trim().split("\\s+");

            boolean ghostPing = false;
            for (String role : oldRoles) {
                if (!newContent.toLowerCase().contains(role.toLowerCase())) {
                    ghostPing = true;
                }
            }

            boolean verifyGhostTag = false;
            if (ghostPing) {
                for (Role role : guild.getRoles()) {
                    if (oldMessage.contains(role.getAsMention().replace("&", ""))) {
                        if (!role.isMentionable()) continue;
                        oldMessage = oldMessage.replace(role.getAsMention().replace("&", ""), role.getAsMention());
                        verifyGhostTag = true;
                    }
                }
                if (!verifyGhostTag) {
                    for (Member foundMember : guild.getMembers()) {
                        if (oldMessage.contains(foundMember.getAsMention().replace("!", ""))) {

                            User foundUser = foundMember.getUser();
                            if (foundUser.isFake() || foundUser.isBot()) continue;
                            if (foundUser.getId().equals(author.getId())) continue;

                            verifyGhostTag = true;
                            break;
                        }
                    }
                }


            }

            if (verifyGhostTag) {
                CriticalAlert cAlert = new CriticalAlert();

                String alertFormat = "**User:** %s (%s)\n **Message:** %s";
                cAlert.setDescription(String.format(alertFormat, author.getAsTag(), author.getId(), oldMessage))
                        .setAuthor("Ghost Tag", null, jda.getSelfUser().getEffectiveAvatarUrl());
                channel.sendMessage(cAlert.build(null)).queue();
            }
        }

    }

    private void deleteMessage(GuildMessageDeleteEvent event) {
        long messageId = event.getMessageIdLong();
        TextChannel channel = event.getChannel();
        JDA jda = channel.getJDA();
        TextChannel logChannel = SourceChannel.MESSAGE_LOG.resolve(jda);
        Document record = log.find(new Document("id", messageId)).first();
        if (record == null) {
            return;
        }

        String authorName = record.getString("author");
        long authorId = record.getLong("authorId");
        Member member = channel.getGuild().getMemberById(authorId);
        User author;
        if (member != null) {
            author = member.getUser();
        } else {
            return;
        }
        User deletedByUser = Utility.getUserWhoDeletedMessage(guild, author);

        String authorFormat = "%s (%d)";
        authorFormat = String.format(authorFormat, authorName, authorId);

        String deletedByUserFormat = "%s (%d)";
        String deletedBy = String.format(deletedByUserFormat, deletedByUser.getAsTag(), deletedByUser.getIdLong());
        deletedBy = deletedByUser == author ? "Self" : deletedBy;

        String message = record.getString("message");
        message = message.isEmpty() ? "N/A" : message;

        String dateTime = "%s at %s";
        dateTime = String.format(dateTime, record.getString("date"), record.getString("time"));

        String channelFormat = "%s (%d)";
        channelFormat = String.format(channelFormat, channel.getName(), channel.getIdLong());

        String format = "**Author:** %s\n **Deleted By:** %s (This may be incorrect)\n**Message:** %s\n**Channel:** %s\n**Message Sent On:** %s";

        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Message Deleted").setDescription(String.format(format, authorFormat, deletedBy, message, channelFormat, dateTime));
        if (record.getString("attachment") != null) {
            alert.setThumbnail(record.getString("attachment"));
        }

        message = message.replaceAll("<@&(\\d+)>", "<@$1>")
                .replaceAll("<@!(\\d+)>", "<@$1>");

        logChannel.sendMessage(alert.build(author)).queue();

        if (!SourceRole.ignoresModeration(member)) {
            message = Utility.containsLink(guild, message) != null ? Utility.containsLink(guild, message) : message;
        }

        Matcher codeMatcher = CodeBlockListener.pattern.matcher(message);

        ArrayList<TextChannel> ignoreGhost = new ArrayList<>();

        Arrays.stream(SourceChannel.ignoreGhost).forEach(sourceChannel -> {
            ignoreGhost.add(sourceChannel.resolve(event.getJDA()));
        });

        if (!CapsListener.checkCaps(message, null) && !codeMatcher.find()
                && !ignoreGhost.contains(channel) && deletedByUser == author) {

            if (SourceRole.ignoresModeration(member)) return;
            Matcher matcher = pingPattern.matcher(message.replaceAll("\\s+", "\n"));

            boolean ghostTag = false;


            if (matcher.find()) {
                for (Role role : guild.getRoles()) {
                    if (message.contains(role.getAsMention().replace("&", ""))) {
                        if (!role.isMentionable()) continue;
                        message = message.replace(role.getAsMention().replace("&", ""), role.getAsMention());
                        ghostTag = true;
                    }
                }
                if (!ghostTag) {
                    for (Member foundMember : guild.getMembers()) {
                        if (message.contains(foundMember.getAsMention().replace("!", ""))) {

                            User foundUser = foundMember.getUser();
                            if (foundUser.isFake() || foundUser.isBot()) continue;
                            if (foundUser.getId().equals(author.getId())) continue;

                            ghostTag = true;
                            break;
                        }
                    }
                }

            }
            if (ghostTag) {
                CriticalAlert cAlert = new CriticalAlert();
                String alertFormat = "**User:** %s (%s)\n **Message:** %s";
                cAlert.setDescription(String.format(alertFormat, author.getAsTag(), author.getId(), message))
                        .setAuthor("Ghost Tag", null, jda.getSelfUser().getEffectiveAvatarUrl());
                channel.sendMessage(cAlert.build(null)).queue();
            }
        }

        log.deleteOne(record);
    }

    private void processMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        User user = member.getUser();
        if (user.isBot() || user.isFake()) {
            return;
        }

        //Ping the new user in the verify channel so they know what they have to do
        TextChannel channel = SourceChannel.AGREE.resolve(guild.getJDA());
        channel.sendMessage(user.getAsMention()).queue(m -> m.delete().queue());

        String id = user.getId();
        Document recoveredQuery = new Document().append("id", id);
        Document recovered = recovery.find(recoveredQuery).first();
        //Restores user data if applicable or creates new data
        if (recovered != null) {
            recovery.deleteMany(recovered);
            recovered.remove("erase");
            userdata.insertOne(recovered);
            new SourceProfile(recovered);
        } else {
            new SourceProfile(user);
        }

        Document query = getCollection("PunishmentHandler").find(new Document("ID", user.getId()).append("TYPE", "TEMPMUTE")).first();
        if (query != null) {
            guild.addRoleToMember(member, SourceRole.MUTED.resolve(guild.getJDA())).queue();
        }
    }

    private void processUserLeave(GuildMemberLeaveEvent event) {
        User user = event.getUser();
        if (user.isBot() || user.isFake()) {
            return;
        }
        Document dataQuery = new Document().append("id", user.getId());
        Document data = userdata.find(dataQuery).first();

        if (data == null) {
            return;
        }

        SourceProfile userProfile = new SourceProfile(user);
        userProfile.deleteProfile();

        long now = System.currentTimeMillis();
        long eraseAfter = Utility.millisFromTimeString("14d");
        long eraseDate = now + eraseAfter;
        data.append("erase", eraseDate);
        recovery.insertOne(data);
    }

    public Document getTagCategories() {
        MongoCollection settings = getCollection("Settings");

        Document categoryListDocument = (Document) settings.find(new Document("name", "categories")).first();


        if (categoryListDocument == null) {
            ArrayList<String> categoryList = new ArrayList<>();
            categoryList.add("Uncategorized");

            categoryListDocument = new Document("name", "categories")
                    .append("categoryList", categoryList);
            settings.insertOne(categoryListDocument);
        }
        return categoryListDocument;
    }

    public void updateTagCategories(Document newCategory) {
        MongoCollection settings = getCollection("Settings");
        settings.updateOne(getTagCategories(), new Document("$set", newCategory));
    }

}