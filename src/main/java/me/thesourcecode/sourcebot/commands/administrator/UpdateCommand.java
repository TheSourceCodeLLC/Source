package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class UpdateCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "update",
            "Updates the bot from GitHub and automatically restarts.",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER).allowControllers();

    @Override
    public Message execute(Source source, Message message, String[] args) {
        final User user = message.getAuthor();
        try {
            final SuccessAlert alert = new SuccessAlert();
            Runtime.getRuntime().exec("/bin/bash ../update.sh");
            alert.setDescription("The bot has been queued for updating!");
            return new MessageBuilder(alert.build(user)).build();
        } catch (Exception e) {
            final CriticalAlert alert = new CriticalAlert();
            alert.setDescription("There was some error updating the bot!");
            alert.addField("Exception in thread:", e.getMessage(), false);
            return new MessageBuilder(alert.build(user)).build();
        }
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
