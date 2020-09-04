package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;


public class TagEditCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "edit",
            "Edits a tag's description",
            "<name> <new description>",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEVELOPERS_STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        SourceTag sourceTag;

        try {
            sourceTag = new SourceTag(args[0]);
        } catch (NullPointerException ex) {
            CommonAlerts alerts = new CommonAlerts();
            return new MessageBuilder(alerts.invalidTag(user)).build();
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        description = description.replaceAll("\u200b", "");

        if (description.contains("@everyone")) {
            description = description.replace("@everyone", "@\u200beveryone");
        }

        if (description.length() > 1024) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("Tag descriptions can only be 1024 or less characters!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        if (description.equals(sourceTag.getDescription())) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("That tag already has that description!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        sourceTag.setDescription(user.getId(), description);

        SuccessAlert success = new SuccessAlert();
        success.setDescription("Successfully changed the description of the tag " + sourceTag.getName());
        return new MessageBuilder(success.build(user)).build();
    }
}
