package me.thesourcecode.sourcebot.commands.administrator.reactionroles;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.ReactionRole;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class ReactionRoleRestrictCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "restrict",
            "Restricts a reaction role to a specific role",
            "<emote> <role id>",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }


    @Override
    public Message execute(Source source, Message message, String[] args) {
        final User user = message.getAuthor();
        final Guild guild = source.getGuild();

        String unicode = args[0];
        if (unicode.startsWith("<")) {
            unicode = unicode.substring(2, unicode.lastIndexOf(":"));
        }

        final String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);

        final String roleId = args[1];
        final Role restrictRole = guild.getRoleById(roleId);

        if (restrictRole == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There is no role with that id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        ReactionRole reactionRole = ReactionRole.getRRoleByShortCode(shortcode);
        if (reactionRole == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There is no reaction role using this emoji!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String currentRestrictedRole = reactionRole.getRestrictedRoleId();
        if (currentRestrictedRole != null && currentRestrictedRole.equalsIgnoreCase(roleId)) {
            reactionRole.setRestrictedRoleId(null);

            SuccessAlert alert = new SuccessAlert();
            alert.setDescription("Successfully unrestricted the reaction role!");
            return new MessageBuilder(alert.build(user)).build();
        }

        reactionRole.setRestrictedRoleId(roleId);

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("Successfully restricted the reaction role!");
        return new MessageBuilder(alert.build(user)).build();
    }
}
