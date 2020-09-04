package me.thesourcecode.sourcebot.listener;

import com.mongodb.client.MongoCollection;
import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceTempban;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.bson.Document;

import java.util.List;

public class ReportsListener extends AbstractListener<GuildMessageReactionAddEvent> {

    private final DatabaseManager dbManager;

    public ReportsListener(Source source) {
        super(GuildMessageReactionAddEvent.class);

        this.dbManager = source.getDatabaseManager();
    }

    @Override
    public void accept(GuildMessageReactionAddEvent event) {
        TextChannel channel = event.getChannel();
        User user = event.getUser();
        if (channel != SourceChannel.REPORTS.resolve(event.getJDA())) return;
        if (user.isBot() || user.isFake()) return;

        try {
            Message reportMessage = channel.retrieveMessageById(event.getMessageId()).complete();

            MessageReaction reaction = event.getReaction();
            MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
            if (reactionEmote.isEmote()) {
                return;
            }
            String unicode = reactionEmote.getName();

            final String WHITE_CHECK_MARK = ":white_check_mark:", X = ":x:";

            String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);
            if (!shortcode.equalsIgnoreCase(WHITE_CHECK_MARK) && !shortcode.equalsIgnoreCase(X)) return;

            List<User> reactedUsers = reportMessage.getReactions().get(0).retrieveUsers().complete();
            if (!reactedUsers.contains(event.getJDA().getSelfUser())) return;

            if (reportMessage.getEmbeds().size() == 0) return;
            MessageEmbed reportEmbed = reportMessage.getEmbeds().get(0);


            MessageEmbed.AuthorInfo author = reportEmbed.getAuthor();
            MessageEmbed.Footer footer = reportEmbed.getFooter();

            boolean isReport = author.getName().contains("Report");

            String description = reportEmbed.getDescription();

            String userTagId = user.getAsTag() + " (" + user.getId() + ")";
            if (shortcode.equalsIgnoreCase(X)) {
                description += "\n\n**Marked as Invalid By:** " + userTagId;
            } else {
                description += "\n\n**Handled By:** " + userTagId;
                if (!isReport) {

                    String targetId = description.substring(description.indexOf("#") + 1);
                    targetId = targetId.substring(targetId.indexOf("(") + 1);
                    targetId = targetId.substring(0, targetId.indexOf(")")).trim();

                    if (event.getGuild().getMemberById(targetId) != null) {
                        boolean isInChat = author.getName().equalsIgnoreCase("Potential Advertisement");
                        applyAdvertisingPunishment(user.getId(), targetId, isInChat);
                    }

                }
            }

            EmbedBuilder newEmbed = new EmbedBuilder()
                    .setAuthor(author.getName(), author.getUrl(), author.getIconUrl())
                    .setColor(SourceColor.GREEN.asColor())
                    .setDescription(description)
                    .setFooter(footer.getText(), footer.getIconUrl());
            newEmbed.getFields().addAll(reportEmbed.getFields());
            reportMessage.editMessage(newEmbed.build()).queue();

            reportMessage.clearReactions().queue();

            if (isReport) {
                String authorName = author.getName();
                String id = authorName.substring(authorName.lastIndexOf("Id:") + 3).trim();

                MongoCollection<Document> userReports = dbManager.getCollection("UserReports");
                Document query = new Document("REPORT_ID", Long.valueOf(id));

                Document reportDocument = userReports.find(query).first();
                if (reportDocument == null) return;

                reportDocument.put("HANDLED_BY", shortcode.equalsIgnoreCase(X) ? user.getId() + " INVALID" : user.getId());
                userReports.updateOne(query, new Document("$set", reportDocument));

            }

        } catch (Exception ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("Something appears to have gone wrong! Please message Forbidden.");
            ex.printStackTrace();
        }
    }

    private void applyAdvertisingPunishment(String staffId, String targetId, boolean isInChat) {
        try {
            String reason = "Advertising in Status (Punishments vary based on your punishment history)";
            reason = isInChat ? "Advertising in Chat (Punishments vary based on your punishment history)" : reason;

            SourceTempban sourceTempban = new SourceTempban(staffId, targetId, "1 Week", reason);
            sourceTempban.sendIncidentEmbed();
            sourceTempban.execute();

            long decayTime = (2592000000L * 8) + System.currentTimeMillis();
            Utility.addPointsToUser(targetId, 77.8, decayTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
