package me.thesourcecode.sourcebot.commands.developer.role;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import java.util.HashMap;

public class RoleListCommand extends Command {
    private static final CommandInfo INFO = new CommandInfo(
            "list",
            "Lists all the roles you can add to a user.",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEVELOPERS_STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {

        HashMap<Integer, Role> roleList = RoleCommand.getRoleList(source, message);
        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);

        for (Integer key : roleList.keySet()) {
            alert.appendDescription("**" + key + ".** " + roleList.get(key).getName() + "\n");
        }
        message.getChannel().sendMessage(alert.build(message.getAuthor())).queue();
        return null;
    }
}
