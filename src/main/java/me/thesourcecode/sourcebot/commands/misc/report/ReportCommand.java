package me.thesourcecode.sourcebot.commands.misc.report;

import com.mongodb.client.MongoCollection;
import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

public class ReportCommand extends Command {

    private static CommandInfo INFO = new CommandInfo("report",
            "Reports the specified user.",
            "(list|edit|get) <@user|id|name> <reason>",
            CommandInfo.Category.GENERAL);

    public ReportCommand() {
        registerSubcommand(new ReportListCommand());
        registerSubcommand(new ReportEditCommand());
        registerSubcommand(new ReportGetCommand());
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();
        Member target = Utility.getMemberByIdentifier(guild, args[0]);
        if (target == null) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidUser(user)).build();
        }

        User targetUser = target.getUser();
        if (targetUser == user) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not report yourself!");
            return new MessageBuilder(alert.build(user)).build();
        }
        if (targetUser.isBot() || targetUser.isFake()) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not report Discord bots!");
            return new MessageBuilder(alert.build(user)).build();
        }
        TextChannel reportChannel = SourceChannel.REPORTS.resolve(source.getJda());
        DatabaseManager databaseManager = source.getDatabaseManager();
        MongoCollection userReports = databaseManager.getCollection("UserReports");

        String reason = String.join(" ", args);
        reason = reason.substring(reason.indexOf(" ") + 1);
        String userTag = user.getAsTag();
        String targetTag = targetUser.getAsTag();

        String format = "**Reported By:** %s (%s)\n" +
                "**Reported User:** %s (%s)\n" +
                "**Channel:** %s (%s)" +
                "\n**Reason:** %s";


        if (message.getChannelType() == ChannelType.PRIVATE) {
            format = format.replace("**Channel:** %s (%s)", "**Channel:** DM");
        } else {
            TextChannel channel = message.getTextChannel();
            format = format.replace("**Channel:** %s (%s)", "**Channel:** " + channel.getName() + " (" + channel.getId() + ")");
        }

        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setDescription(String.format(format, userTag, user.getId(), targetTag, targetUser.getId(), reason));

        long id = userReports.countDocuments() + 1;
        reportChannel.sendMessage((source.isBeta() ? "" : "@everyone ") + "A new report has been made on **"
                + targetUser.getAsTag() + "** by: **" + user.getAsTag() + "**").complete();
        Message newReport = reportChannel.sendMessage(cAlert.build(targetUser, "Report | Id: " + id)).complete();
        newReport.addReaction(EmojiParser.parseToUnicode(":white_check_mark:")).complete();
        newReport.addReaction(EmojiParser.parseToUnicode(":x:")).complete();

        Document mongoReport = new Document("REPORT_ID", id)
                .append("USER_ID", user.getId())
                .append("TARGET_ID", targetUser.getId())
                .append("REASON", reason)
                .append("MESSAGE_ID", newReport.getId())
                .append("HANDLED_BY", null)
                .append("TIME", System.currentTimeMillis());
        userReports.insertOne(mongoReport);

        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("User successfully reported, you may get the report id of this report by using `!report list`!");

        try {
            PrivateChannel pChannel = user.openPrivateChannel().complete();
            format = "You have reported " + targetTag + "\n**Reason:** %s";
            cAlert.setDescription(String.format(format, reason));
            pChannel.sendMessage(cAlert.build(targetUser, "Report | Id: " + id)).complete();
        } catch (Exception ignored) {
        }

        return new MessageBuilder(sAlert.build(source.getJda().getSelfUser(), "Success!")).build();
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
