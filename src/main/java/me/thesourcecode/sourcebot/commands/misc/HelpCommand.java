package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Set;

public class HelpCommand extends Command {
    private static CommandInfo INFO = new CommandInfo(
            "help",
            "Displays this message.",
            "(command name)",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Set<CommandInfo> commands = CommandHandler.getCommands().getCommandInfo();

        InfoAlert helpAlert = new InfoAlert();

        Guild guild = source.getGuild();
        Member member = guild.getMember(user);
        List<SourceRole> memberRoles = SourceRole.getRolesFor(member);

        if (args.length == 0) {
            StringBuilder admin = new StringBuilder(),
                    moderator = new StringBuilder(),
                    developer = new StringBuilder(),
                    general = new StringBuilder(),
                    economy = new StringBuilder(),
                    development = new StringBuilder();

            commands.forEach(commandInfo -> {
                CommandInfo.Category category = commandInfo.getCategory();
                String categoryFormat = "`%s`, ";

                switch (category) {
                    case ADMIN:
                        admin.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    case MODERATOR:
                        moderator.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    case DEVELOPER:
                        developer.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    case DEVELOPMENT:
                        development.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    case ECONOMY:
                        economy.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    case GENERAL:
                        general.append(String.format(categoryFormat, commandInfo.getLabel()));
                        break;
                    default:
                        break;
                }

            });

            if (memberRoles.contains(SourceRole.ADMIN)) {
                addCategoryToField(helpAlert, "Admin", admin);
            }

            if (SourceRole.ignoresModeration(member)) {
                addCategoryToField(helpAlert, "Moderation", moderator);
            }

            if (memberRoles.contains(SourceRole.DEV)) {
                addCategoryToField(helpAlert, "Developer", developer);
            }

            addCategoryToField(helpAlert, "Development", development);
            addCategoryToField(helpAlert, "Economy", economy);

            general.append("`?<tag name>`, ");
            addCategoryToField(helpAlert, "General", general);

            helpAlert.setDescription("Here is a list of all of my available commands!\n" +
                    "Command Arguments: `<>` = Required, `()` = Optional\n" +
                    "My prefix is `!` | For more info about a specific command do `!help <command>`");
        } else {
            String commandQuery = args[0].toLowerCase();
            Command foundCommand = CommandHandler.getCommands().getCommand(commandQuery);

            if (foundCommand == null) {
                return new MessageBuilder(invalidCommand(commandQuery).build(user)).build();
            }

            String parentLabel = foundCommand.getInfo().getLabel() + " ";
            StringBuilder cmdDescSB = new StringBuilder("**!").append(parentLabel);

            CommandInfo commandInfo = foundCommand.getInfo();
            if (args.length > 1) {
                Command subCommand;
                for (int retrieveArg = 1; retrieveArg < args.length; retrieveArg++) {
                    String subCommandName = args[retrieveArg].toLowerCase();

                    subCommand = commandInfo.getSubcommands().getCommand(subCommandName);
                    if (subCommand == null) {
                        commandQuery = String.join(" ", args);
                        return new MessageBuilder(invalidCommand(commandQuery).build(user)).build();
                    }
                    commandInfo = subCommand.getInfo();
                    cmdDescSB.append(commandInfo.getLabel()).append(" ");
                }

            }

            String commandDescription = cmdDescSB.toString();
            String commandFormat = "%s** - %s\n**Aliases:** %s";

            String aliases = commandInfo.getAliases().length == 0 ? "N/A" : String.join(" ", commandInfo.getAliases());
            commandDescription += String.format(commandFormat, commandInfo.getArguments(), commandInfo.getDescription(), aliases);

            helpAlert.setDescription("Command Arguments: `<>` = Required, `()` = Optional\n\n" + commandDescription);

        }

        message.getChannel().sendMessage(helpAlert.build(user, "Help Menu")).queue();

        return null;
    }

    private CriticalAlert invalidCommand(String commandQuery) {
        CriticalAlert alert = new CriticalAlert();

        String sanitizedCommandQuery = commandQuery.replace("`", "");
        alert.setTitle("Uh Oh!").setDescription("Uh Oh! `" + sanitizedCommandQuery + "` is not a valid command!");
        return alert;
    }

    private InfoAlert addCategoryToField(InfoAlert alert, String categoryName, StringBuilder categoryDescription) {
        String formattedCatDesc = categoryDescription.toString().trim();
        formattedCatDesc = formattedCatDesc.substring(0, formattedCatDesc.length() - 1);

        alert.addField(categoryName, formattedCatDesc, false);
        return alert;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}