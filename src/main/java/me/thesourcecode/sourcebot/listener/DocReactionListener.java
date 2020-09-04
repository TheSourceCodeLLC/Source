package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.List;

public class DocReactionListener extends AbstractListener<MessageReactionAddEvent> {

    public DocReactionListener() {
        super(MessageReactionAddEvent.class);
    }

    @Override
    public void accept(MessageReactionAddEvent event) {
        MessageChannel channel = event.getChannel();
        String messageId = event.getMessageId();

        try {
            Message message = channel.retrieveMessageById(messageId).complete();

            User reactor = event.getUser();
            if (reactor == null) return;

            if (message.getAuthor() != event.getJDA().getSelfUser()) return;
            if (message.getEmbeds().size() == 0) return;

            MessageEmbed messageEmbed = message.getEmbeds().get(0);

            MessageEmbed.Footer footer = messageEmbed.getFooter();
            if (footer == null) return;

            String footerText = footer.getText();
            if (footerText == null || !footerText.contains("Ran By:")) return;

            String ranBy = footerText.replace("Ran By:", "").trim();

            if (!ranBy.equals(reactor.getAsTag())) {
                return;
            }


            String unicode = event.getReactionEmote().getName();
            String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);
            if (shortcode.equalsIgnoreCase(":x:")) {
                message.delete().queue();

                MessageHistory messageHistory = channel.getHistoryBefore(messageId, 10).complete();
                List<Message> retrievedMessages = messageHistory.getRetrievedHistory();

                for (Message retrievedMsg : retrievedMessages) {
                    if (retrievedMsg.getAuthor() == reactor || SourceRole.ignoresModeration(event.getMember())) {
                        String rawContent = retrievedMsg.getContentRaw();

                        if (rawContent.startsWith("!") && Utility.isCommand(retrievedMsg)) {
                            retrievedMsg.delete().queue();
                        }

                    }
                }

            }
        } catch (Exception ignored) {

        }
    }
}
