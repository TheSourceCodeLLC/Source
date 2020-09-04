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

public class StopCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "stop",
            "Stops the bot.",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully stopped the bot!");
        message.getChannel().sendMessage(sAlert.build(user)).queue();
        try {
            Utility.safeStop();

            String jarname = source.isBeta() ? "sourcebeta" : "sourcebot";
            Runtime.getRuntime().exec("systemctl --user stop " + jarname);
        } catch (Exception ex) {
            ex.printStackTrace();

            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("ERROR").setDescription("The bot was unable to stop");
            message.getChannel().sendMessage(alert.build(user)).queue();
            return null;
        }
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
