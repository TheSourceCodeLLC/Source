package me.thesourcecode.sourcebot.commands.administrator.giveaway;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GiveawayCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "giveaway",
            "Allows a user to start, stop, or reroll a giveaway.",
            "<start|reroll|stop>",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER)
            .withAliases("ga");

    public GiveawayCommand() {
        registerSubcommand(new GiveawayStartCommand());
        registerSubcommand(new GiveawayRerollCommand());
        registerSubcommand(new GiveawayStopCommand());
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Invalid Usage!")
                .setDescription("Syntax: " + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments());
        return new MessageBuilder(alert.build(message.getAuthor())).build();
    }

}


