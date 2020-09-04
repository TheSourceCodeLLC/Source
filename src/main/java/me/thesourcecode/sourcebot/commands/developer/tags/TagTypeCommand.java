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

public class TagTypeCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "type",
            "Creates and removes tag aliases",
            "<name> <embed|text>",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEVELOPERS_STAFF);

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

        String type = args[1].toLowerCase();
        if (!type.equals("embed") && !type.equals("text")) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not specify a valid type!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        sourceTag.setType(user.getId(), type.equals("embed"));

        SuccessAlert success = new SuccessAlert();
        String successMsg = "Successfully changed the type to " + type + " for the tag " + sourceTag.getName() + "!";
        success.setDescription(successMsg);
        return new MessageBuilder(success.build(user)).build();
    }
}
