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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class ReactionRoleRemoveCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "remove",
            "Allows a user to remove a reaction roles",
            "<emote>",
            CommandInfo.Category.ADMIN
    ).withControlRoles(SourceRole.ADMIN, SourceRole.OWNER)
            .withAliases("delete");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }


    @Override
    public Message execute(Source source, Message message, String[] args) {
        final User user = message.getAuthor();

        String unicode = args[0];
        if (unicode.startsWith("<")) {
            unicode = unicode.substring(2, unicode.lastIndexOf(":"));
        }

        final String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);

        ReactionRole reactionRole = ReactionRole.getRRoleByShortCode(shortcode);
        if (reactionRole == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There is no reaction role using this emoji!");
            return new MessageBuilder(alert.build(user)).build();
        }

        reactionRole.deleteReactionRole();

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("Successfully deleted the specified reaction role!");
        return new MessageBuilder(alert.build(user)).build();

    }
}
