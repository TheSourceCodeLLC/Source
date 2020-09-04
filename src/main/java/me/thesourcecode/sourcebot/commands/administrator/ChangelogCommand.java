package me.thesourcecode.sourcebot.commands.administrator;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.Alert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.entities.*;

public class ChangelogCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "changelog",
            "Post a changelog update",
            "<update>",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.STAFF).asGuildOnly();

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {

        final String log = String.join(" ", args);
        final Alert info = new InfoAlert();

        info.setTitle("Source Update!");
        info.setDescription("We've got some new features for you!");
        info.addField("Overview:", log, false);
        final User author = message.getAuthor();
        final MessageEmbed embed = info.build(author);
        TextChannel changelog = SourceChannel.CHANGELOG.resolve(message.getJDA());
        changelog.sendMessage(embed).queue(m -> {
            m.addReaction(EmojiParser.parseToUnicode(":white_check_mark:")).queue();
            m.addReaction(EmojiParser.parseToUnicode(":x:")).queue();
        });
        if (message.getChannelType() != ChannelType.PRIVATE) message.delete().queue();
        return null;
    }

}
