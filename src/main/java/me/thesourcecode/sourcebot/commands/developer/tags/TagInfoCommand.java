package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TagInfoCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "info",
            "Sends information about specified tag's such as who created it",
            "<name>",
            CommandInfo.Category.DEVELOPER
    ).withUsageChannels(SourceChannel.COMMANDS, SourceChannel.DEV_LEADERS, SourceChannel.DEVELOPERS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        String tagName = args[0].toLowerCase();

        SourceTag sourceTag;

        try {
            sourceTag = new SourceTag(tagName);
        } catch (NullPointerException ex) {
            CommonAlerts alerts = new CommonAlerts();
            return new MessageBuilder(alerts.invalidTag(user)).build();
        }

        String format = "**Name:** %s\n" +
                "**Aliases:** %s\n" +
                "**Uses:** %d\n" +
                "**Type:** %s\n\n" +
                "**Created By:** %s\n" +
                "**Created On:** %s";

        Guild guild = source.getGuild();

        List<String> getEdited = sourceTag.getEditIds();
        StringBuilder sb = new StringBuilder();

        getEdited.forEach(foundId -> {
            String foundUser = foundId;
            Member foundMember = Utility.getMemberByIdentifier(guild, foundId);
            if (foundMember != null) {
                foundUser = foundMember.getUser().getAsTag();
            }
            sb.append("`").append(foundUser).append("`, ");
        });

        String editedBy = sb.toString().trim();
        if (!editedBy.isBlank()) {
            editedBy = editedBy.substring(0, editedBy.length() - 1);
            format += "\n**Edited By:** " + editedBy;
        }


        Member member = Utility.getMemberByIdentifier(guild, sourceTag.getCreatorId());

        String createdBy = sourceTag.getCreatorId();
        if (member != null) {
            createdBy = member.getUser().getAsTag();
        }

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");
        Date date = new Date(sourceTag.getTimeCreatedInMs());

        String aliases = sourceTag.getAliases().size() != 0 ? Utility.formatList(sourceTag.getAliases()) : "N/A";
        int uses = sourceTag.getUses();
        String type = sourceTag.getType().substring(0, 1).toUpperCase() + sourceTag.getType().substring(1);

        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        alert.setDescription(String.format(format, sourceTag.getName(), aliases, uses, type, createdBy, dateFormat.format(date)));
        message.getChannel().sendMessage(alert.build(user)).queue();

        return null;
    }
}
