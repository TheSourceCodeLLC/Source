package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import net.dv8tion.jda.api.entities.Message;

public class TagCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "tag",
            "Manages tags",
            "<source|list|info|create|delete|edit|rename|alias|category|type>",
            CommandInfo.Category.DEVELOPER
    ).withAliases("tags");

    public TagCommand() {

        registerSubcommand(new TagCreateCommand());
        registerSubcommand(new TagDeleteCommand());

        registerSubcommand(new TagEditCommand());
        registerSubcommand(new TagRenameCommand());
        registerSubcommand(new TagCategoryCommand());

        registerSubcommand(new TagTypeCommand());
        registerSubcommand(new TagAliasCommand());

        registerSubcommand(new TagListCommand());
        registerSubcommand(new TagSourceCommand());
        registerSubcommand(new TagInfoCommand());

    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        return null;
    }

}
