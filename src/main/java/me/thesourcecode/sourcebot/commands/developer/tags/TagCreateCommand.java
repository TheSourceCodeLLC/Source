package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;

public class TagCreateCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "create",
            "Creates tags",
            "<name> <description>",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEVELOPERS_STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        DatabaseManager dbManager = source.getDatabaseManager();

        String tagName = args[0].toLowerCase();
        SourceTag sourceTag;
        try {
            sourceTag = new SourceTag(tagName);

            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("It seems that there is already a tag, or a tag with an alias, with that name !");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        } catch (NullPointerException ignored) {
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (description.length() > 1024) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("Tag descriptions can only be 1024 or less characters!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        if (description.contains("@everyone")) {
            description = description.replace("@everyone", "@\u200beveryone");
        }

        // Creates tag
        SourceTag newTag = new SourceTag(user.getId(), tagName, description);

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully created a new tag!");
        return new MessageBuilder(alert.build(user)).build();
    }
}
