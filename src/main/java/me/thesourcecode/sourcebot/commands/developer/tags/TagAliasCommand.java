package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

public class TagAliasCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "alias",
            "Creates and removes tag aliases",
            "<add|remove> <name> <alias>",
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

        boolean add = false;
        switch (args[0].toLowerCase()) {
            case "create":
            case "add":
                add = true;
            case "delete":
            case "remove":
                break;
            default:
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("You did not specify if you wanted to add or remove a tag alias!");
                MessageEmbed embed = alert.build(message.getAuthor());
                return new MessageBuilder(embed).build();
        }

        String tagName = args[1].toLowerCase();
        SourceTag sourceTag;
        try {
            sourceTag = new SourceTag(tagName);
        } catch (NullPointerException ex) {
            CommonAlerts alerts = new CommonAlerts();
            return new MessageBuilder(alerts.invalidTag(user)).build();
        }

        List<String> aliases = sourceTag.getAliases();

        String modifyAlias = args[2].toLowerCase();
        if (modifyAlias.equalsIgnoreCase(sourceTag.getName())) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not modify the tag name via this command! Instead use the `!tag rename` command");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }
        if ((add && (aliases.contains(modifyAlias) || modifyAlias.equalsIgnoreCase(sourceTag.getName()))
                || (!add && !aliases.contains(modifyAlias)))) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription(add ? "There is already an alias with that name on this tag!" :
                    "This tag does not contain an alias with that name!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        if (add) {
            try {
                SourceTag tagExists = new SourceTag(modifyAlias);

                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("A tag already has that alias or name!");
                MessageEmbed embed = alert.build(message.getAuthor());
                return new MessageBuilder(embed).build();
            } catch (NullPointerException ignored) {

            }

            aliases.add(modifyAlias);
        } else {
            aliases.remove(modifyAlias);
        }

        sourceTag.setAliases(user.getId(), aliases);

        SuccessAlert success = new SuccessAlert();
        String successMsg = "Successfully " + (add ? "added" : "removed") + " an alias " + (add ? "to " : "from ") + sourceTag.getName() + "!";
        success.setDescription(successMsg);
        return new MessageBuilder(success.build(user)).build();

    }
}
