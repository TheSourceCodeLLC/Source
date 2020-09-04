package me.thesourcecode.sourcebot.commands.misc.report;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.util.Iterator;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.orderBy;

public class ReportListCommand extends Command {

    private static CommandInfo INFO = new CommandInfo("list",
            "Allows a user to see all of the reports they have reported",
            "(page number)",
            CommandInfo.Category.GENERAL);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        User target = user;
        Guild guild = source.getGuild();
        DatabaseManager dbManager = source.getDatabaseManager();
        PrivateChannel channel = user.openPrivateChannel().complete();

        int page = 1;
        if (args.length > 0) {
            Member member = Utility.getMemberByIdentifier(guild, user.getId());
            Member findMember = Utility.getMemberByIdentifier(guild, args[0]);
            if (!SourceRole.ignoresModeration(member) && findMember != null) {

                target = findMember.getUser();
                if (args.length == 2) {

                    page = getPageNum(args[1]);
                    if (page == -1) {
                        return invalidPageNumber(user).build();
                    }
                }
            } else {
                page = getPageNum(args[1]);
                if (page == -1) {
                    return invalidPageNumber(user).build();
                }
            }

        }

        MongoCollection reports = dbManager.getCollection("UserReports");
        Document query = new Document("USER_ID", target.getId());
        FindIterable<Document> found = reports.find(query)
                .skip(page == 1 ? 0 : (page * 5) - 5)
                .limit(5)
                .sort(orderBy(ascending("TIME")));
        int maxPages = (int) Math.ceil((double) reports.countDocuments(query) / 5);

        if (maxPages == 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You have reported no one!");

            if (target != user) {
                alert.setDescription(target.getAsTag() + " has reported no one!");
            }
            return new MessageBuilder(alert.build(user)).build();
        }

        Iterator it = found.iterator();

        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        alert.setDescription("Here are all of the reports you have reported:\n\n");
        if (target != user) {
            alert.setDescription("Here are all of the reports " + target.getAsTag() + " has reported:\n\n");
        }


        while (it.hasNext()) {
            Document foundReport = (Document) it.next();
            String reason = foundReport.getString("REASON");
            long id = foundReport.getLong("REPORT_ID");

            String reportedUser = foundReport.getString("TARGET_ID");
            Member member = Utility.getMemberByIdentifier(guild, reportedUser);
            if (member != null) {
                reportedUser = member.getUser().getAsTag() + " (" + member.getUser().getId() + ")";
            }
            String format = "**Report Id: %s.** %s: *Reason: %s*\n";

            alert.appendDescription(String.format(format, id, reportedUser, reason));
        }
        alert.appendDescription("\nPage " + page + " of " + maxPages);

        try {
            channel.sendMessage(alert.build(user, "Report List")).complete();
        } catch (Exception ex) {
            CriticalAlert cAlert = new CriticalAlert();
            cAlert.setTitle("Uh Oh!").setDescription("Your DMs must be open to use this command!");
            return new MessageBuilder(cAlert.build(user)).build();
        }

        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("Successfully sent you your report list!");
        if (target != user) {
            alert.setDescription("Successfully sent you " + target.getAsTag() + "'s report list!");
        }

        if (channel.getType() != ChannelType.PRIVATE) {
            return new MessageBuilder(sAlert.build(user)).build();
        }

        return null;
    }

    private int getPageNum(String var) {
        try {
            return Integer.parseInt(var);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private MessageBuilder invalidPageNumber(User user) {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Uh Oh!").setDescription("You did not enter a valid page number!");
        return new MessageBuilder(alert.build(user));
    }
}
