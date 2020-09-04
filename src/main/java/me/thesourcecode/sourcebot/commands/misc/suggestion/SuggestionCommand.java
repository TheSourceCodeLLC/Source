package me.thesourcecode.sourcebot.commands.misc.suggestion;

import me.theforbiddenai.trellowrapperkotlin.objects.TrelloList;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceTList;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceSuggestion;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class SuggestionCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "suggest",
            "Allows a user to submit a suggestion/bug for source/discord.",
            "(edit) <tsc|source|website> <suggestion|bug> <title> | (description)",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS, SourceChannel.SUGGESTIONS)
            .withAliases("suggestion", "bug");

    public SuggestionCommand() {
        registerSubcommand(new SuggestionEditCommand());
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        String board = args[0].toLowerCase();
        String suggestionBug = args[1].toLowerCase();
        switch (suggestionBug) {
            case "fix":
            case "bug":
                suggestionBug = "bug";
                break;
            case "add":
            case "suggestion":
                suggestionBug = "suggestion";
                break;
            default:
                return new MessageBuilder(invalidUsage().build(user)).build();
        }

        boolean isBug = suggestionBug.equalsIgnoreCase("bug");

        SourceTList sourceTList;

        switch (board) {
            case "tsc":
                sourceTList = isBug ? SourceTList.TSC_ISSUES : SourceTList.TSC_SUGGESTIONS;
                break;
            case "website":
                sourceTList = isBug ? SourceTList.WEBSITE_BUGS : SourceTList.WEBSITE_SUGGESTIONS;
                break;
            case "source":
                sourceTList = isBug ? SourceTList.SOURCE_BUGS : SourceTList.SOURCE_SUGGESTIONS;
                break;
            default:
                return new MessageBuilder(invalidUsage().build(user)).build();
        }

        TrelloList trelloList = sourceTList.asList();
        SourceSuggestion.CardType suggestionType = isBug ? SourceSuggestion.CardType.BUG : SourceSuggestion.CardType.SUGGESTION;

        String titleDesc = String.join(" ", args).substring(args[0].length() + args[1].length() + 1).trim();
        String[] suggestionArgs = titleDesc.split("\\|");

        String title = suggestionArgs[0];
        String description = suggestionArgs.length > 1 ? suggestionArgs[1] : "";
        String reporterInfo = user.getAsTag() + " (" + user.getId() + ")";

        SourceSuggestion sourceSuggestion = new SourceSuggestion(trelloList, suggestionType, title, description, reporterInfo);
        sourceSuggestion.sendCardEmbed();

        TextChannel suggestionsChannel = SourceChannel.SUGGESTIONS.resolve(source.getJda());

        if (message.getChannel() != suggestionsChannel) {
            SuccessAlert sAlert = new SuccessAlert();
            String sBug = isBug ? "bug report" : "suggestion";
            sAlert.setDescription("You have successfully submitted your " + sBug);
            return new MessageBuilder(sAlert.build(user)).build();
        }
        message.delete().queue();
        return null;
    }


    private CriticalAlert invalidUsage() {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Invalid Usage!")
                .setDescription("Syntax: " + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments());
        return alert;

    }

}
