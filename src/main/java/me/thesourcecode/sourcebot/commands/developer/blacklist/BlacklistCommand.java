package me.thesourcecode.sourcebot.commands.developer.blacklist;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

public class BlacklistCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "blacklist",
            "Blacklists the specified user from the specified category.",
            "<development|status>",
            CommandInfo.Category.DEVELOPER)
            .withControlRoles(SourceRole.DEVELOPERS_STAFF)
            .withAliases("bl");

    public BlacklistCommand() {
        registerSubcommand(new BlacklistDevelopmentCommand());
        registerSubcommand(new BlacklistStatusCommand());
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {

        String category = args[0].toLowerCase();
        if (INFO.getSubcommands().getCommand(category) == null) {
            Command devBlacklist = INFO.getSubcommands().getCommand("development");

            String argsString = devBlacklist.getInfo().getLabel() + " " + String.join(" ", args);
            args = argsString.split("\\s+");

            return onCommand(source, message, args);

        }

        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Invalid Usage!")
                .setDescription("Syntax: " + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments());
        return new MessageBuilder(alert.build(message.getAuthor())).build();
    }

}
