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
import net.dv8tion.jda.api.entities.User;

public class ReactionRoleAddCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "add",
            "Allows a user to add a reaction roles",
            "<role id> <emote>",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER)
            .withAliases("create");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }


    @Override
    public Message execute(Source source, Message message, String[] args) {
        final Guild guild = source.getGuild();
        final User user = message.getAuthor();

        final String roleId = args[0];
        String unicode = args[1];
        if (unicode.startsWith("<")) {
            unicode = unicode.substring(2, unicode.lastIndexOf(":"));
        }


        final String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);

        if (guild.getRoleById(roleId) == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("Failed to find a role with the given id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        try {
            ReactionRole reactionRole = new ReactionRole(shortcode, roleId);
            reactionRole.createReactionRole();

            SuccessAlert alert = new SuccessAlert();
            alert.setDescription("Successfully created a reaction role!");
            return new MessageBuilder(alert.build(user)).build();
        } catch (IllegalArgumentException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There already is a reaction role using this emoji!");
            return new MessageBuilder(alert.build(user)).build();
        }

    }
}
