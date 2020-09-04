package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

public class CaseCommand extends Command {
    private static CommandInfo INFO = new CommandInfo(
            "case",
            "Displays information for the given case id.",
            "<case id>",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS).withAliases("incident");

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();
        int caseId;
        try {
            caseId = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid case id!");
            return new MessageBuilder(alert.build(user)).build();
        }
        DatabaseManager dbManager = source.getDatabaseManager();
        Document query = new Document("CASE_ID", caseId);
        Document incidentReport = dbManager.getCollection("IncidentReports").find(query).first();
        if (incidentReport == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("I could not find an incident with the id of " + caseId + "!");
            return new MessageBuilder(alert.build(user)).build();
        }
        String type = incidentReport.getString("TYPE");

        if (type.equalsIgnoreCase("DEV_CLEAR")) type = "CLEAR";

        String staffId = incidentReport.getString("STAFF_ID");
        String reason = incidentReport.getString("REASON");
        String targetId = incidentReport.getString("TARGET_ID");
        String dateTime = incidentReport.getString("DATE_TIME");

        if (type.equalsIgnoreCase("ROLE UPDATE")) type = "Role Update";
        else type = type.substring(0, 1) + type.toLowerCase().substring(1);

        Member staff = guild.getMemberById(staffId);
        Member target = guild.getMemberById(targetId);
        String sTag = staff == null ? staffId : staff.getUser().getAsTag() + " (" + staffId + ")";
        String tTag = target == null ? targetId : target.getUser().getAsTag() + " (" + targetId + ")";
        TextChannel targetChannel = guild.getTextChannelById(targetId);
        if (type.equals("Clear"))
            tTag = targetChannel == null ? targetId : targetChannel.getName() + " (" + targetId + ")";


        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);

        String action = type + "ed";
        String check = type.toLowerCase();
        if (check.contains("ban")) action = type + "ned";
        else if (check.contains("mute")) action = type + "d";
        else if (check.equalsIgnoreCase("Role Update")) {
            String rAction = incidentReport.getString("ACTION");
            action = "Role " + (rAction.equals("ADD") ? "Added" : "Removed");
        }

        String removedString = "\n**" + action + (type.equalsIgnoreCase("Clear") ? " Channel" : " User") + "**: ";
        if (check.equalsIgnoreCase("ROLE UPDATE")) {
            removedString = removedString.replaceFirst("User", "From");
        }
        alert.setDescription("**" + action + " By:** " + sTag +
                removedString + tTag);

        switch (type) {
            case "Tempmute":
            case "Tempban":
            case "Blacklist":
                String duration = incidentReport.getString("DURATION");
                alert.appendDescription("\n**Duration:** " + duration);
                break;
            case "Clear":
                String amount = incidentReport.getString("MESSAGE_AMOUNT");
                alert.appendDescription("\n**Amount of Messages Cleared:** " + amount);
                break;
            case "Role Update":
                Role role = guild.getRoleById(targetId);
                String sRole = role == null ? targetId : role.getName();
                alert.appendDescription("\n**" + action + "**: " + sRole);
                break;
        }

        alert.appendDescription("\n**Reason:** " + reason.trim() +
                "\n**Date & Time:** " + dateTime.replaceFirst(" ", " at ") + " EST");

        if (incidentReport.getString("TYPE").equalsIgnoreCase("DEV_CLEAR")) type = "Dev Clear";
        String header = type + " | Id: " + caseId;

        message.getChannel().sendMessage(alert.build(user, header)).queue();
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
