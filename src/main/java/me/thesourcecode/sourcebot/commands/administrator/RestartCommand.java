package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class RestartCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "restart",
            "Restarts the bot.",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully restarted the bot!");
        message.getChannel().sendMessage(sAlert.build(user)).queue();

        try {
            Utility.safeStop();

            String jarname = source.isBeta() ? "sourcebeta" : "sourcebot";

            Runtime.getRuntime().exec("systemctl --user restart " + jarname);
        } catch (Exception ex) {
            ex.printStackTrace();

            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("ERROR").setDescription("The bot was unable to restart");
            message.getChannel().sendMessage(alert.build(user)).queue();
            return null;
        }
        return null;
    }
}
