package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceController;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceSuggestion;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class SuggestionVoteListener extends AbstractListener<GuildMessageReceivedEvent> {
    private final String STAR = ":star:", UP = ":white_check_mark:", DOWN = ":x:", POOP = ":hankey:";
    private final Map<Long, Map<String, Integer>> ideaReactionMap = new HashMap<>();
    private final int DOWNVOTE_THRESH = 10;
    private final int UPVOTE_THRESH = 10;

    private final TextChannel suggestionsChannel;


    public SuggestionVoteListener(Source source) {
        super(GuildMessageReceivedEvent.class);
        this.suggestionsChannel = SourceChannel.SUGGESTIONS.resolve(source.getJda());
        readExistingSuggestions();
    }


    @Override
    public void accept(GuildMessageReceivedEvent event) {

        final TextChannel messageChannel = event.getChannel();
        if (messageChannel != suggestionsChannel) return;

        final User user = event.getAuthor();
        final Message message = event.getMessage();
        final String contextRaw = message.getContentRaw();

        if (Utility.isCommand(message)) return;

        if (!user.isBot() && !user.isFake() && SourceRole.ignoresModeration(event.getMember())
                && contextRaw.contains("--ignore")) return;

        MessageType type = message.getType();
        if (type != MessageType.DEFAULT) {
            return;
        }

        if (message.getEmbeds().size() > 0) {
            MessageEmbed embed = message.getEmbeds().get(0);

            try {
                int embedRGB = embed.getColor().getRGB();
                if (embedRGB == SourceColor.GREEN.asColor().getRGB()
                        || embedRGB == SourceColor.RED.asColor().getRGB()) return;
            } catch (Exception ignored) {

            }

        }

        String starUnicode = EmojiParser.parseToUnicode(STAR);
        String checkUnicode = EmojiParser.parseToUnicode(UP);
        String xUnicode = EmojiParser.parseToUnicode(DOWN);
        String poopUnicode = EmojiParser.parseToUnicode(POOP);
        Stream.of(
                checkUnicode,
                xUnicode,
                starUnicode,
                poopUnicode
        ).map(message::addReaction).forEach(RestAction::queue);
    }


    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new SuggestionReactionAddListener().listen(listener);
        new SuggestionReactionRemoveListener().listen(listener);
    }

    private void removeDoubleVote(User user, String shortcode, Message message) {
        MessageReaction mrCheck = message.getReactions().get(0);
        MessageReaction mrX = message.getReactions().get(1);

        List<User> mrCheckUsers = mrCheck.retrieveUsers().complete();
        List<User> mrXUsers = mrX.retrieveUsers().complete();

        if (shortcode.equalsIgnoreCase(UP)) {
            if (mrXUsers.contains(user)) {
                mrX.removeReaction(user).queue();
            }
        } else if (shortcode.equalsIgnoreCase(DOWN)) {
            if (mrCheckUsers.contains(user)) {
                mrCheck.removeReaction(user).queue();
            }
        }
    }

    private void readExistingSuggestions() {
        suggestionsChannel.getIterableHistory().forEach(message -> {
            long messageID = message.getIdLong();
            AtomicInteger star = new AtomicInteger();
            AtomicInteger poop = new AtomicInteger();
            AtomicInteger up = new AtomicInteger();
            AtomicInteger down = new AtomicInteger();
            message.getReactions().forEach(r -> {
                MessageReaction.ReactionEmote e = r.getReactionEmote();
                if (e.isEmote()) {
                    return;
                }
                String uni = e.getName();
                String shortcode = EmojiParser.parseToAliases(uni, EmojiParser.FitzpatrickAction.REMOVE);
                switch (shortcode) {
                    case STAR:
                        star.getAndIncrement();
                        break;
                    case UP:
                        up.getAndIncrement();
                        break;
                    case DOWN:
                        down.getAndIncrement();
                        break;
                    case POOP:
                        poop.getAndIncrement();
                        break;
                }
            });
            // -1 to remove bot votes
            setStarCount(messageID, star.get() - 1);
            setUpvoteCount(messageID, up.get() - 1);
            setDownvoteCount(messageID, down.get() - 1);
            setPoopCount(messageID, star.get() - 1);
        });
    }

    private void deleteSuggestion(Message message, boolean reactor) {
        long messageID = message.getIdLong();
        int upvotes = getUpvotes(messageID);
        int downvotes = getDownvotes(messageID);
        int poop = getPoop(messageID);


        SourceSuggestion sourceSuggestion = new SourceSuggestion(message);

        String reporterId = sourceSuggestion.getReporterId();

        Member member = Utility.getMemberByIdentifier(message.getGuild(), reporterId);
        if (member == null) return;

        String listName = sourceSuggestion.getTrelloList().getName();
        String title = sourceSuggestion.getTitle();
        String description = sourceSuggestion.getDescription();

        String suggestionDescription = "**List:** " + listName +
                "\n**Title:** " + title;
        if (!description.isBlank()) suggestionDescription += "\n**Description:** " + description;

        String voteFormat = "**Upvotes:** %d\n**Downvotes:** %d \n**Poop:** %d";

        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Suggestion Deleted!")
                .setDescription("Your suggestion was deleted!")
                .addField("Suggestion:", suggestionDescription, false)
                .addField("Votes:", String.format(voteFormat, upvotes, downvotes, poop), true);

        User author = member.getUser();
        if (!reactor) {
            author.openPrivateChannel()
                    .queue(privateChannel -> privateChannel.sendMessage(alert.build(author)).queue());
        }

        sourceSuggestion.deleteCardAndEmbed();
        ideaReactionMap.remove(messageID);
    }

    private int getReactionCount(long messageID, String reaction) {
        return ideaReactionMap
                .computeIfAbsent(messageID, $ -> new HashMap<>())
                .computeIfAbsent(reaction, $ -> 0);
    }

    private int getStars(long messageID) {
        return getReactionCount(messageID, STAR);
    }

    private int getPoop(long messageID) {
        return getReactionCount(messageID, POOP);
    }

    private int getUpvotes(long messageID) {
        return getReactionCount(messageID, UP);
    }

    private int getDownvotes(long messageID) {
        return getReactionCount(messageID, DOWN);
    }

    private void setPoopCount(long messageID, int count) {
        setReactionCount(messageID, POOP, count);
    }

    private void setReactionCount(long messageID, String reaction, int count) {
        ideaReactionMap.computeIfAbsent(messageID, $ -> new HashMap<>()).put(reaction, count);
    }

    private void setStarCount(long messageID, int count) {
        setReactionCount(messageID, STAR, count);
    }

    private void setUpvoteCount(long messageID, int count) {
        setReactionCount(messageID, UP, count);
    }

    private void setDownvoteCount(long messageID, int count) {
        setReactionCount(messageID, DOWN, count);
    }

    private final class SuggestionReactionAddListener extends AbstractListener<GuildMessageReactionAddEvent> {

        SuggestionReactionAddListener() {
            super(GuildMessageReactionAddEvent.class);
        }

        @Override
        public void accept(GuildMessageReactionAddEvent event) {
            TextChannel channel = event.getChannel();
            if (!channel.equals(suggestionsChannel)) {
                return;
            }

            User reactor = event.getUser();
            if (reactor.isBot() || reactor.isFake()) {
                return;
            }
            long messageID = event.getMessageIdLong();
            Message message = channel.retrieveMessageById(messageID).complete();
            if (message == null) {
                return;
            }

            final String contextRaw = message.getContentRaw();
            if (contextRaw.contains("--ignore") && SourceRole.ignoresModeration(message.getMember())) {
                return;
            }

            MessageReaction reaction = event.getReaction();
            MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
            if (reactionEmote.isEmote()) {
                return;
            }
            String unicode = reactionEmote.getName();

            String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);
            int upvotes = getUpvotes(messageID);
            int downvotes = getDownvotes(messageID);
            int stars = getStars(messageID);
            int poop = getPoop(messageID);
            switch (shortcode) {
                case STAR:
                    if (SourceController.isValidController(reactor)) {
                        stars++;
                        setStarCount(messageID, stars);

                        SourceSuggestion sourceSuggestion = new SourceSuggestion(message);
                        try {
                            sourceSuggestion.approveCard();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    } else reaction.removeReaction(reactor).queue();
                    break;
                case DOWN:
                    downvotes++;
                    setDownvoteCount(messageID, downvotes);

                    removeDoubleVote(reactor, shortcode, message);
                    if (downvotes < DOWNVOTE_THRESH) {
                        return;
                    }
                    if (stars > 0) {
                        return;
                    }
                    deleteSuggestion(message, false);
                    return;
                case UP:
                    upvotes++;
                    setUpvoteCount(messageID, upvotes);

                    removeDoubleVote(reactor, shortcode, message);

                    if (upvotes >= UPVOTE_THRESH) {
                        SourceSuggestion sourceSuggestion = new SourceSuggestion(message);
                        try {
                            sourceSuggestion.approveCard();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case POOP:
                    SourceSuggestion sourceSuggestion = new SourceSuggestion(message);
                    String reporterId = sourceSuggestion.getReporterId();
                    if (SourceController.isValidController(reactor) || reactor.getId().equals(reporterId)) {
                        poop++;
                        setPoopCount(messageID, poop);
                        deleteSuggestion(message, reactor.getId().equals(reporterId));
                    } else reaction.removeReaction(reactor).queue();
                    break;
            }
        }


    }

    private final class SuggestionReactionRemoveListener extends AbstractListener<GuildMessageReactionRemoveEvent> {

        SuggestionReactionRemoveListener() {
            super(GuildMessageReactionRemoveEvent.class);
        }

        @Override
        public void accept(GuildMessageReactionRemoveEvent event) {
            TextChannel channel = event.getChannel();
            if (!channel.equals(suggestionsChannel)) {
                return;
            }
            User reactor = event.getUser();
            if (reactor.isBot() || reactor.isFake()) {
                return;
            }
            long messageID = event.getMessageIdLong();
            Message message = channel.retrieveMessageById(messageID).complete();
            if (message == null) {
                return;
            }

            final String contextRaw = message.getContentRaw();
            if (contextRaw.contains("--ignore") && SourceRole.ignoresModeration(message.getMember())) {
                return;
            }

            MessageReaction reaction = event.getReaction();
            MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
            if (reactionEmote.isEmote()) {
                return;
            }
            String unicode = reactionEmote.getName();
            String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);
            int upvotes = getUpvotes(messageID);
            int downvotes = getDownvotes(messageID);
            int stars = getStars(messageID);
            switch (shortcode) {
                case STAR:
                    if (SourceController.isValidController(reactor)) {
                        stars--;
                        setStarCount(messageID, stars);
                    }
                    return;
                case DOWN:
                    downvotes--;
                    setDownvoteCount(messageID, downvotes);
                    return;
                case UP:
                    upvotes--;
                    setUpvoteCount(messageID, upvotes);
            }
        }
    }
}
