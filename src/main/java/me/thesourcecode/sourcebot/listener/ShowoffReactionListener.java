package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ShowoffReactionListener extends AbstractListener<GuildMessageReceivedEvent> {
    private final String UP = ":arrow_up:";
    private final TextChannel projects;

    public ShowoffReactionListener(Source source) {
        super(GuildMessageReceivedEvent.class);
        this.projects = SourceChannel.PROJECTS.resolve(source.getJda());
    }

    @Override
    public void accept(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();
        if (!channel.equals(projects)) {
            return;
        }
        Message message = event.getMessage();

        MessageType type = message.getType();
        if (type != MessageType.DEFAULT) {
            return;
        }
        String upVoteUnicode = EmojiParser.parseToUnicode(UP);
        message.addReaction(upVoteUnicode).queue();
    }
}
