package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.theforbiddenai.jenkinsparserkotlin.entities.Information;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.commands.misc.doc.JenkinsHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class DocSelectionListener extends AbstractListener<MessageReceivedEvent> {

    public static final ConcurrentHashMap<User, Map.Entry<Message, JenkinsHandler>> docStorageCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<User, List<Information>> selectionStorageCache = new ConcurrentHashMap<>();

    public DocSelectionListener() {
        super(MessageReceivedEvent.class);
    }

    @Override
    public void accept(MessageReceivedEvent event) {
        Message selectionMessage = event.getMessage();
        String messageContent = selectionMessage.getContentRaw();

        if (messageContent.startsWith("!")) return;

        User user = event.getAuthor();
        if (!docStorageCache.containsKey(user)) {
            return;
        }

        Message docSelectionMenu = docStorageCache.get(user).getKey();
        if (event.getChannelType() != ChannelType.PRIVATE && event.getChannel() != docSelectionMenu.getChannel()) {
            return;
        }

        List<Information> informationList = selectionStorageCache.get(user);
        JenkinsHandler jenkins = docStorageCache.get(user).getValue();

        selectionStorageCache.remove(user);
        docStorageCache.remove(user);

        if (messageContent.equalsIgnoreCase("cancel")) {
            docSelectionMenu.delete().queue();
            return;
        }

        if (event.getChannelType() != ChannelType.PRIVATE) {
            selectionMessage.delete().queue();
        }

        int selectedId;
        Information information;
        try {
            selectedId = Integer.parseInt(messageContent) - 1;
            information = informationList.get(selectedId);
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            invalidId(user, jenkins, docSelectionMenu);
            return;
        }


        MessageEmbed.AuthorInfo selectionMenuAuthor = docSelectionMenu.getEmbeds().get(0).getAuthor();

        EmbedBuilder documentationEmbed = new EmbedBuilder()
                .setColor(SourceColor.BLUE.asColor())
                .setAuthor(selectionMenuAuthor.getName(), null, selectionMenuAuthor.getIconUrl());

        docSelectionMenu.editMessage(jenkins.createDocumentationEmbed(documentationEmbed, information).build()).queue();
        docSelectionMenu.addReaction(EmojiParser.parseToUnicode(":x:")).queue();

    }

    /**
     * Posts the invalid id embed
     *
     * @param user    The user who entered the invalid id
     * @param jenkins The javadoc object that contains the doc url
     * @param message The doc selection menu message
     */
    private void invalidId(User user, JenkinsHandler jenkins, Message message) {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Uh Oh!")
                .setDescription("It appears you have entered an invalid selection id! For the javadocs click "
                        + MarkdownUtil.maskedLink("here", jenkins.getDocURL()));

        message.editMessage(alert.build(user)).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
    }

}
