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

public class TagRenameCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "rename",
            "Edits a tag's name",
            "<name> <replacement>",
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

        if (args.length != 2) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("Tag names can only be one word!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        String newTagName = args[1].toLowerCase();
        if (newTagName.equalsIgnoreCase(sourceTag.getName()) || sourceTag.getAliases().contains(newTagName)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("That tag already has that name, or an alias with that name!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        SourceTag checkIfTagExists;
        try {
            checkIfTagExists = new SourceTag(newTagName);

            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There is already a tag with that name, or an alias of a tag with that name!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        } catch (NullPointerException ignored) {
        }


        sourceTag.setName(user.getId(), newTagName);

        SuccessAlert success = new SuccessAlert();
        success.setDescription("Successfully changed the name of the tag " + tagName + " to " + args[1]);
        return new MessageBuilder(success.build(user)).build();

    }
}
