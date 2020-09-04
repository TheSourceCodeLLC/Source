package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class TagSourceCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "source",
            "Gets tag's raw description",
            "<name>",
            CommandInfo.Category.DEVELOPER
    );

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        String tagName = args[0].toLowerCase();
        SourceTag sourceTag;

        try {
            sourceTag = new SourceTag(tagName);
        } catch (NullPointerException ex) {
            CommonAlerts alerts = new CommonAlerts();
            return new MessageBuilder(alerts.invalidTag(user)).build();
        }

        String description = "```markdown\n" + sourceTag.getDescription()
                .replaceAll("```", "`\u200b``") + "\n```";

        message.getChannel().sendMessage(description).queue();
        return null;
    }
}
