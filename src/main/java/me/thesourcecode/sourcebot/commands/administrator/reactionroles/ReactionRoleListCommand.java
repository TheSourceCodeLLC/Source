package me.thesourcecode.sourcebot.commands.administrator.reactionroles;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import me.thesourcecode.sourcebot.api.objects.ReactionRole;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.LinkedList;

public class ReactionRoleListCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "list",
            "Sends the user a list of the reaction roles with their respective emotes.",
            "(page)",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }


    @Override
    public Message execute(Source source, Message message, String[] args) {
        final User user = message.getAuthor();
        CommonAlerts commonAlerts = new CommonAlerts();

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                return new MessageBuilder(commonAlerts.invalidPage(user)).build();
            }
        }

        LinkedList<ReactionRole> reactionRoles = ReactionRole.getAllReactionRoles();
        if (reactionRoles.size() == 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There are no reaction roles!");
            return new MessageBuilder(alert.build(user)).build();
        }

        int maxPages = (int) Math.ceil((double) reactionRoles.size() / 10);
        if (page > maxPages) {
            return new MessageBuilder(commonAlerts.invalidPage(user)).build();
        }

        InfoAlert alert = new InfoAlert();
        reactionRoles.stream()
                .skip(page == 1 ? 0 : (page * 10) - 10)
                .limit(10)
                .forEach(reactionRole -> {
                    String shortcode = reactionRole.getShortcode();
                    String roleId = reactionRole.getRoleId();

                    alert.appendDescription(shortcode + " - " + roleId + "\n");
                });
        alert.appendDescription("\nPage " + page + " of " + maxPages);

        message.getChannel().sendMessage(alert.build(user, "Reaction Role List")).queue();
        return null;
    }
}
