package me.thesourcecode.sourcebot.listener;

import com.mongodb.client.MongoCollection;
import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class UserActivityListener extends AbstractListener<UserActivityStartEvent> {

    private final static List<String> tldList = new ArrayList<>();
    private final static HashMap<String, Pattern> tldPatterns = new HashMap<>();
    private final static HashMap<String, ArrayList<String>> previousCaughtLinks = new HashMap<>();
    private final Source source;
    private final DatabaseManager dbManager;

    public UserActivityListener(Source source) {
        super(UserActivityStartEvent.class);

        this.source = source;
        this.dbManager = source.getDatabaseManager();

        initializeTLDList();
        initializeURLBlacklist();

        checkAllMemberActivities();
    }

    @Override
    public void accept(UserActivityStartEvent event) {
        User user = event.getUser();
        Activity activity = event.getNewActivity();

        if (SourceRole.ignoresModeration(event.getMember())) return;

        if (checkActivityForAdvertisements(user, activity)) {
            String statusType = activity.getType() == Activity.ActivityType.CUSTOM_STATUS ? "Custom Status" : "Activity";

            String description = "**" + statusType + ":** " + activity.getName();
            sendEmbed(user, description);
        }
    }

    private void initializeTLDList() {
        try {
            org.jsoup.nodes.Document tldDocument = Jsoup.connect("https://data.iana.org/TLD/tlds-alpha-by-domain.txt").get();

            String documentBody = tldDocument.body().text();
            documentBody = documentBody.substring(documentBody.indexOf("UTC") + 3).trim();

            tldList.addAll(Arrays.asList(documentBody.split("\\s+")));

            tldList.forEach(tld -> {
                tld = tld.toLowerCase().trim();

                Pattern tldPattern = Pattern.compile("\\w+?[.](" + tld + ")");
                tldPatterns.put(tld, tldPattern);
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeURLBlacklist() {
        MongoCollection<Document> settings = dbManager.getCollection("Settings");
        Document urlBlackListDocument = settings.find(new Document("name", "urlBlacklist")).first();
        if (urlBlackListDocument == null) {
            urlBlackListDocument = new Document("name", "urlBlacklist")
                    .append("urls", new ArrayList<String>());

            settings.insertOne(urlBlackListDocument);
        }

        ArrayList<String> urlBlacklist = Utility.urlBlacklist;
        urlBlacklist.addAll((ArrayList<String>) urlBlackListDocument.get("urls"));
    }

    // This should only be called on bot start
    private void checkAllMemberActivities() {
        Guild guild = source.getGuild();
        guild.getMemberCache().forEach(member -> {
            User user = member.getUser();
            if (user.isBot() || user.isFake() || SourceRole.ignoresModeration(member)) return;

            StringBuilder descriptionSb = new StringBuilder();
            member.getActivities().forEach(activity -> {

                if (checkActivityForAdvertisements(user, activity)) {
                    String statusType = activity.getType() == Activity.ActivityType.CUSTOM_STATUS ? "Custom Status" : "Activity";

                    descriptionSb.append("**").append(statusType).append(":** ").append(activity.getName()).append("\n");
                }
            });

            String embedDescription = descriptionSb.toString().trim();
            if (embedDescription.length() != 0) {
                sendEmbed(user, embedDescription);
            }
        });
    }

    private boolean checkActivityForAdvertisements(User user, Activity activity) {
        ArrayList<String> userPrevCaughtLinks = previousCaughtLinks.containsKey(user.getId()) ?
                previousCaughtLinks.get(user.getId()) : new ArrayList<>();

        ArrayList<String> caughtLinks = new ArrayList<>();

        String activityName = activity.getName().toLowerCase().trim();
        String[] activityNameArgs = activityName.split("\\s+");

        ArrayList<String> urlBlacklist = Utility.urlBlacklist;

        tldList.forEach(topLevelDomain -> {
            Pattern tldPattern = tldPatterns.get(topLevelDomain.toLowerCase());
            if (tldPattern == null) return;

            Arrays.asList(activityNameArgs).forEach(nameArg -> {
                if (tldPattern.matcher(nameArg).matches()) {
                    caughtLinks.add(nameArg);
                }
            });
        });

        caughtLinks.removeIf(urlBlacklist::contains);
        caughtLinks.removeIf(userPrevCaughtLinks::contains);

        if (caughtLinks.size() > 0) {
            userPrevCaughtLinks.addAll(caughtLinks);
            if (previousCaughtLinks.get(user.getId()) != null) {
                previousCaughtLinks.replace(user.getId(), userPrevCaughtLinks);
            } else {
                previousCaughtLinks.put(user.getId(), caughtLinks);
            }
            return true;
        }

        return false;
    }

    private void sendEmbed(User user, String description) {
        ColoredAlert alert = new ColoredAlert(SourceColor.ORANGE);

        String userInfo = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n";
        alert.setDescription(userInfo + description);

        TextChannel reports = SourceChannel.REPORTS.resolve(user.getJDA());
        Message message = reports.sendMessage(alert.build(user, "Advertisement In Status")).complete();

        message.addReaction(EmojiParser.parseToUnicode(":white_check_mark:")).complete();
        message.addReaction(EmojiParser.parseToUnicode(":x:")).complete();
    }
}
