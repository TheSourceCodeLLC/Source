package me.thesourcecode.sourcebot.api.objects;

import com.vdurmont.emoji.EmojiParser;
import me.theforbiddenai.trellowrapperkotlin.TrelloApi;
import me.theforbiddenai.trellowrapperkotlin.objects.Board;
import me.theforbiddenai.trellowrapperkotlin.objects.Card;
import me.theforbiddenai.trellowrapperkotlin.objects.TrelloList;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceTList;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class SourceSuggestion {

    private final static Source source = Source.getInstance();
    private final static TrelloApi trelloApi = source.getTrelloApi();
    private final static TextChannel suggestionsChannel = SourceChannel.SUGGESTIONS.resolve(source.getJda());

    boolean isApproved;
    private Board trelloBoard;
    private TrelloList trelloList;
    private CardType cardType;
    private String title;
    private String description;
    private String reporterInfo;
    private String trelloCardId = null;
    private Message cardMessage = null;
    private String trelloLink = null;

    public SourceSuggestion(@NotNull TrelloList trelloList,
                            @NotNull CardType cardType,
                            @NotNull String title,
                            @NotNull String description,
                            @NotNull String reporterInfo) {
        this.trelloList = trelloList;
        this.cardType = cardType;
        this.title = MarkdownSanitizer.sanitize(title, MarkdownSanitizer.SanitizationStrategy.ESCAPE);
        this.description = MarkdownSanitizer.sanitize(description, MarkdownSanitizer.SanitizationStrategy.ESCAPE);
        this.reporterInfo = reporterInfo;
        this.isApproved = false;

        if (!reporterInfo.matches(".+#\\d{4} \\(\\d+\\)")) {
            throw new IllegalArgumentException("Reporter info must be in the format {name}#{discrim} ({id})");
        }

        trelloBoard = trelloList.getBoard();
    }

    public SourceSuggestion(@NotNull Message cardMessage) {
        this.cardMessage = cardMessage;
        resolveFromMessage();
    }

    public SourceSuggestion(@NotNull Card card) {
        this.trelloCardId = card.getId();
        this.trelloLink = card.getShortUrl();
        resolveFromTrelloCard(card);
    }

    /**
     * Removes fixed/dead bugs/suggestions from the suggestions channel and removes the message id from their trello cards
     */
    public static void removeImplementedSuggestions() {
        try {
            TrelloList dead = SourceTList.DEAD.asList();
            TrelloList fixed = SourceTList.FIXED.asList();

            if (dead == null || fixed == null) {
                throw new NullPointerException("Either the dead or fixed trello list is null!");
            }

            source.getExecutorService().scheduleAtFixedRate(() -> {

                ArrayList<Card> foundCards = new ArrayList<>(Arrays.asList(fixed.getCards()));
                foundCards.addAll(Arrays.asList(dead.getCards()));

                foundCards.forEach(card -> {
                    try {
                        if (card.getDesc().contains("**Message Id:**")) {
                            SourceSuggestion sourceSuggestion = new SourceSuggestion(card);
                            sourceSuggestion.suggestionImplemented();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }, 0, 60, TimeUnit.MINUTES);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @NotNull
    public String getReporterId() {
        String reporterId = reporterInfo.replaceAll(".+#\\d{4}", "").trim();
        return reporterId.replaceAll("[()]", "");
    }

    @NotNull
    public CardType getCardType() {
        return cardType;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NotNull String title) {
        this.title = MarkdownSanitizer.sanitize(title, MarkdownSanitizer.SanitizationStrategy.ESCAPE);
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = MarkdownSanitizer.sanitize(description, MarkdownSanitizer.SanitizationStrategy.ESCAPE);
    }

    @NotNull
    public String getReporterInfo() {
        return reporterInfo;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    @Nullable
    public String getTrelloCardId() {
        return trelloCardId;
    }

    public void setTrelloCardId(@NotNull String trelloCardId) {
        this.trelloCardId = trelloCardId;
    }

    @Nullable
    public Message getCardMessage() {
        return cardMessage;
    }

    public void setCardMessage(@Nullable Message cardMessage) {
        this.cardMessage = cardMessage;
    }

    @NotNull
    public TrelloList getTrelloList() {
        return trelloList;
    }

    @Nullable
    public String getTrelloLink() {
        return trelloLink;
    }

    public void setTrelloLink(@NotNull String trelloLink) {
        this.trelloLink = trelloLink;
    }

    /**
     * Sends the card embed to the suggestions channel
     */
    public void sendCardEmbed() {
        Message cardMessage = suggestionsChannel.sendMessage(createCardEmbed()).complete();
        setCardMessage(cardMessage);
    }

    /**
     * Creates the suggestion/bug embed
     */
    private MessageEmbed createCardEmbed() {
        String action = cardType == CardType.BUG ? "Reported" : "Suggested";

        InfoAlert cardAlert = new InfoAlert();
        cardAlert.setDescription("**" + action + " By:** " + reporterInfo +
                "\n**List:** " + trelloList.getName() +
                "\n\n**Title:** " + title);
        if (!description.isEmpty()) {
            cardAlert.appendDescription("\n**Description:** " + description);
        }

        String authorOverride = cardType == CardType.BUG ? "Bug Report" : "Suggestion";
        return cardAlert.build(source.getJda().getSelfUser(), authorOverride);
    }

    /**
     * Updates the trello card and the embed in the suggestions channel
     */
    public void updateCardAndEmbed() {
        if (cardMessage == null) {
            throw new NullPointerException("Can not find the card message object!");
        }

        MessageEmbed cardEmbed = createCardEmbed();
        if (isApproved) {
            SuccessAlert cardAlert = new SuccessAlert();
            cardAlert.setDescription(cardEmbed.getDescription());
            cardAlert.appendDescription("\n\n**Trello Link:** " + trelloLink);

            String authorOverride = cardEmbed.getAuthor().getName();
            cardEmbed = cardAlert.build(source.getJda().getSelfUser(), authorOverride);

            if (trelloCardId != null) {
                try {
                    Card trelloCard = trelloBoard.getCardById(trelloCardId);

                    trelloCard.setName(title);
                    trelloCard.setDesc(createTrelloCardDescription());

                    trelloCard.updateCard();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        Message newCardMessage = cardMessage.editMessage(cardEmbed).complete();
        setCardMessage(newCardMessage);
    }

    /**
     * Deletes the trello card and message
     */
    public void deleteCardAndEmbed() {
        if (cardMessage != null) {
            cardMessage.delete().complete();
        }

        if (trelloCardId != null) {
            try {
                trelloBoard.getCardById(trelloCardId).deleteCard();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Adds the trello link to the card, makes the embed color green, and creates a trello card
     *
     * @throws Exception If the card is already approved
     */
    public void approveCard() throws Exception {
        if (isApproved) {
            throw new Exception("This card has already been approved");
        }
        if (cardMessage == null) {
            throw new NullPointerException("Can not find the card message object!");
        }

        createTrelloCard();

        if (trelloLink == null) {
            throw new NullPointerException("Can not find the trello link for this card!");
        }

        Message newCardMessage;
        if (cardMessage.getEmbeds().size() > 0) {
            MessageEmbed oldCardEmbed = cardMessage.getEmbeds().get(0);

            SuccessAlert cardAlert = new SuccessAlert();
            cardAlert.setDescription(oldCardEmbed.getDescription());
            cardAlert.appendDescription("\n\n**Trello Link:** " + trelloLink);

            String authorOverride = oldCardEmbed.getAuthor().getName();
            MessageEmbed newMessageEmbed = cardAlert.build(source.getJda().getSelfUser(), authorOverride);

            newCardMessage = cardMessage.editMessage(newMessageEmbed).complete();

            cardMessage.getReactions().stream().filter(reaction -> {
                String reactionUnicode = reaction.getReactionEmote().getName();
                String emojiName = EmojiParser.parseToAliases(reactionUnicode, EmojiParser.FitzpatrickAction.REMOVE);

                return emojiName.equalsIgnoreCase(":hankey:") || emojiName.equalsIgnoreCase(":star:");
            }).forEach(reaction -> {
                reaction.retrieveUsers().forEach(user -> reaction.removeReaction(user).queue());
            });
        } else {
            SuccessAlert cardAlert = new SuccessAlert();
            String actionString = cardType == CardType.SUGGESTION ? "Suggested" : "Reported";
            cardAlert.setDescription("**" + actionString + ":** " + reporterInfo + "\n" +
                    "**List:** " + trelloList.getName() + "\n\n" +
                    "**Title:** " + title + "\n" +
                    "**Description:** " + description + "\n\n" +
                    "**Trello Link:** " + trelloLink);

            String alertTitle = cardType == CardType.SUGGESTION ? "Suggestion" : "Bug Report";
            MessageEmbed cardEmbed = cardAlert.build(source.getJda().getSelfUser(), alertTitle);
            newCardMessage = suggestionsChannel.sendMessage(cardEmbed).complete();

            String checkUnicode = EmojiParser.parseToUnicode(":white_check_mark:");
            String xUnicode = EmojiParser.parseToUnicode(":x:");
            Stream.of(
                    checkUnicode,
                    xUnicode
            ).map(newCardMessage::addReaction).forEach(RestAction::queue);

            cardMessage.delete().queue();
        }

        setCardMessage(newCardMessage);
        setApproved(true);
    }

    /**
     * Creates a trello card
     *
     * @throws Exception If the card has already been approved
     */
    private void createTrelloCard() throws Exception {
        if (isApproved) {
            throw new Exception("This card has already been approved!");
        }

        if (cardMessage == null) {
            throw new NullPointerException("Can not find the card message object!");
        }

        String listId = trelloList.getId();
        String desc = createTrelloCardDescription();

        Card trelloCard = new Card(trelloApi, listId, title, desc);
        trelloCard = trelloCard.createCard();

        setTrelloLink(trelloCard.getShortUrl());
        setTrelloCardId(trelloCard.getId());
    }

    /**
     * Resolves a SourceCard from a message
     */
    private void resolveFromMessage() {
        if (cardMessage == null) {
            throw new NullPointerException("Can not find the card message object!");
        }

        if (cardMessage.getEmbeds().size() > 0) {
            retrieveSuggestionInfoFromEmbed(cardMessage.getEmbeds().get(0));

        } else {
            retrieveSuggestionInfoFromMessage(cardMessage);
            trelloBoard = trelloList.getBoard();
        }

    }

    private void retrieveSuggestionInfoFromEmbed(MessageEmbed cardEmbed) {
        String typeString = cardEmbed.getAuthor().getName();

        cardType = typeString.equalsIgnoreCase("Suggestion") ? CardType.SUGGESTION : CardType.BUG;
        isApproved = cardEmbed.getColor().equals(SourceColor.GREEN.asColor());

        String embedDescription = cardEmbed.getDescription();
        String[] descriptionArgs = embedDescription.split("\\*\\*.*:\\*\\*\\s");

        descriptionArgs = Arrays.stream(descriptionArgs)
                .filter(arg -> !arg.isEmpty())
                .map(String::trim)
                .toArray(String[]::new);

        reporterInfo = descriptionArgs[0].trim();
        trelloList = SourceTList.resolveListFromName(descriptionArgs[1]);
        title = descriptionArgs[2].trim();
        description = "";

        if (trelloList == null) {
            throw new IllegalArgumentException("Trello list can not be null");
        }

        switch (descriptionArgs.length) {
            case 4:
                if (!isApproved) {
                    this.description = descriptionArgs[3];
                } else {
                    this.trelloLink = descriptionArgs[3];
                }
                break;
            case 5:
                this.description = descriptionArgs[3];
                this.trelloLink = descriptionArgs[4];
                break;
        }

        trelloBoard = trelloList.getBoard();

        if (trelloLink != null) {
            try {
                this.trelloCardId = Arrays.stream(trelloBoard.getCards())
                        .filter(card -> card.getShortUrl().equalsIgnoreCase(trelloLink))
                        .findFirst().get().getId();
            } catch (NoSuchElementException ex) {
                throw new IllegalArgumentException("Failed to locate a card with the short url of " + trelloLink);
            }
        }
    }

    private void retrieveSuggestionInfoFromMessage(Message message) {
        final String contextRaw = message.getContentRaw();
        final String[] contextArgs = contextRaw.split("\n");
        final User user = message.getAuthor();

        reporterInfo = user.getAsTag() + " (" + user.getId() + ")";
        cardType = CardType.SUGGESTION;
        trelloList = null;
        title = "";
        description = "N/A";
        isApproved = false;

        StringBuilder descriptionSB = new StringBuilder();
        boolean foundProperty = false;
        for (String arg : contextArgs) {
            final String[] argSplit = arg.split("\\s+");
            final String startsWith = argSplit[0]
                    .toLowerCase()
                    .replace(":", "")
                    .trim();

            String modifiedArg = arg.replace(argSplit[0], "").trim();
            switch (startsWith) {
                case "list":
                case "**list**":
                    trelloList = SourceTList.resolveListFromName(modifiedArg);
                    if (trelloList == null || trelloList == SourceTList.DEAD.asList() || trelloList == SourceTList.FIXED.asList()) {
                        trelloList = SourceTList.SOURCE_SUGGESTIONS.asList();
                    }
                    foundProperty = true;
                    break;
                case "type":
                case "**type**":
                    cardType = modifiedArg.contains("suggestion") ? CardType.SUGGESTION : CardType.BUG;
                    foundProperty = true;
                    break;
                case "title":
                case "**title**":
                    title = modifiedArg;
                    foundProperty = true;
                    break;
                case "description":
                case "**description**":
                    description = modifiedArg;
                    foundProperty = true;
                    break;
                default:
                    descriptionSB.append(arg).append("\n");
                    foundProperty = true;
            }

        }

        if (trelloList != null && trelloList.getName().contains("Bugs")) {
            cardType = CardType.BUG;
        }

        if (trelloList == null) {
            trelloList = cardType == CardType.SUGGESTION ? SourceTList.SOURCE_SUGGESTIONS.asList() : SourceTList.SOURCE_BUGS.asList();
        }

        if (!foundProperty) {
            description = contextRaw;
        }

        String descSBToString = descriptionSB.toString().trim();
        if (!descSBToString.isBlank()) {
            description = descSBToString;
        }

        if (title.isBlank() && description.equalsIgnoreCase("N/A")) {
            message.delete().queue();
        }

        if (title.isBlank()) {
            title = user.getName() + "'s " + (cardType == CardType.SUGGESTION ? "Suggestion" : "Bug Report");
        }

        description = MarkdownSanitizer.sanitize(description, MarkdownSanitizer.SanitizationStrategy.ESCAPE);
        title = MarkdownSanitizer.sanitize(title, MarkdownSanitizer.SanitizationStrategy.ESCAPE);

    }

    /**
     * Resolves a SourceCard from a trello card
     *
     * @param card The trello card the information is being pulled from
     */
    private void resolveFromTrelloCard(Card card) {
        String cardDescription = card.getDesc();
        String[] descriptionArgs = cardDescription.split("\\*\\*.*:\\*\\*\\s");

        descriptionArgs = Arrays.stream(descriptionArgs)
                .filter(arg -> !arg.isEmpty())
                .map(String::trim)
                .toArray(String[]::new);

        this.cardType = cardDescription.startsWith("**Reported By:**") ? CardType.BUG : CardType.SUGGESTION;
        this.isApproved = true;
        this.reporterInfo = descriptionArgs[0];
        this.title = descriptionArgs[1];
        this.description = "";

        String messageId = null;

        switch (descriptionArgs.length) {
            case 3:
                messageId = descriptionArgs[2];
                break;
            case 4:
                this.description = descriptionArgs[2];
                messageId = descriptionArgs[3];
                break;
        }
        if (messageId == null) {
            throw new IllegalArgumentException("Unable to find the message associated with this card!");
        }

        try {
            this.cardMessage = suggestionsChannel.retrieveMessageById(messageId).complete();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to find the message associated with this card!");
        }

        trelloBoard = card.getBoard();
        try {
            this.trelloList = trelloApi.getList(card.getIdList());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to find the trello list associated with this card!");
        }

    }

    /**
     * Creates the description for the trello card
     *
     * @return The trello card's description
     */
    private String createTrelloCardDescription() {
        String action = cardType == CardType.BUG ? "Reported" : "Suggested";

        String cardDescription = "**" + action + " By:** " + reporterInfo +
                "\n\n**Title:** " + title;

        if (!description.isEmpty()) {
            cardDescription += "\n**Description:** " + description;
        }

        if (cardMessage != null) {
            cardDescription += "\n\n**Message Id:** " + cardMessage.getId();
        }

        return cardDescription;
    }

    /**
     * Deletes the card message and removes the message id from the trello card
     */
    private void suggestionImplemented() {
        if (cardMessage != null) {
            cardMessage.delete().queue();
        }
        setCardMessage(null);

        try {
            Card trelloCard = trelloBoard.getCardById(trelloCardId);

            trelloCard.setName(title);
            trelloCard.setDesc(createTrelloCardDescription());

            trelloCard.updateCard();
        } catch (Exception ignored) {
        }

    }

    public enum CardType {
        BUG, SUGGESTION
    }

}