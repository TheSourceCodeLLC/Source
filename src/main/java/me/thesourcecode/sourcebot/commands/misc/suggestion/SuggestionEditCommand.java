package me.thesourcecode.sourcebot.commands.misc.suggestion;

import me.theforbiddenai.trellowrapperkotlin.TrelloApi;
import me.theforbiddenai.trellowrapperkotlin.objects.Board;
import me.theforbiddenai.trellowrapperkotlin.objects.Card;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceController;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceSuggestion;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class SuggestionEditCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "edit",
            "Allows a user to edit an approved bug/suggestion.",
            "<message id|trello link> <title|description> <content>",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS, SourceChannel.SUGGESTIONS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        TextChannel suggestions = SourceChannel.SUGGESTIONS.resolve(source.getJda());

        String trelloLink = args[0].endsWith("/") ? args[0].substring(0, args[0].lastIndexOf("/")) : args[0];

        SourceSuggestion sourceSuggestion = null;

        if (!args[0].contains("trello.com")) {
            Message suggestionMessage;
            try {
                suggestionMessage = suggestions.retrieveMessageById(args[0]).complete();

                if (suggestionMessage.getEmbeds().size() == 0) {
                    CriticalAlert alert = new CriticalAlert();
                    alert.setTitle("Uh Oh!")
                            .setDescription("You entered an invalid message id or trello link!");
                    return new MessageBuilder(alert.build(user)).build();
                }
            } catch (IllegalArgumentException ex) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!")
                        .setDescription("You entered an invalid message id or trello link!");
                return new MessageBuilder(alert.build(user)).build();
            }

            sourceSuggestion = new SourceSuggestion(suggestionMessage);
            trelloLink = sourceSuggestion.getTrelloLink();
        }


        if (trelloLink != null) {

            TrelloApi trelloApi = source.getTrelloApi();

            Board trelloBoard = trelloApi.getBoard("sbuNrhD8");
            Card foundCard;

            try {
                String finalLink = trelloLink;
                foundCard = Arrays.stream(trelloBoard.getCards())
                        .filter(card -> card.getShortUrl().equalsIgnoreCase(finalLink))
                        .findFirst().get();
            } catch (NoSuchElementException ex) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!")
                        .setDescription("You entered an invalid trello card link!");
                return new MessageBuilder(alert.build(user)).build();
            }

            sourceSuggestion = new SourceSuggestion(foundCard);

        }

        String reporterId = sourceSuggestion.getReporterId();

        if (!reporterId.equalsIgnoreCase(user.getId()) && !SourceController.isValidController(user)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!")
                    .setDescription("You can not edit someone else's suggestion or bug!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String content = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        switch (args[1].toLowerCase()) {
            case "title":
                sourceSuggestion.setTitle(content);
                break;
            case "desc":
            case "description":
                sourceSuggestion.setDescription(content);
                break;
            default:
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Invalid Usage!")
                        .setDescription("Syntax: " + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments());
                return new MessageBuilder(alert.build(user)).build();
        }

        sourceSuggestion.updateCardAndEmbed();

        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("Successfully edited the specified suggestion/bug!");
        return new MessageBuilder(sAlert.build(user)).build();


    }

}