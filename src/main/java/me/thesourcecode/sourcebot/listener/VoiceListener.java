package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

import java.util.function.Function;

public final class VoiceListener extends AbstractListener<GuildVoiceJoinEvent> {
    private final Function<JDA, TextChannel> channelResolver = jda -> (TextChannel) SourceChannel.VOICE_CHAT.resolve(jda);

    public VoiceListener() {
        super(GuildVoiceJoinEvent.class);
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new VoiceLeave().listen(listener);
    }

    @Override
    public void accept(GuildVoiceJoinEvent event) {
        final JDA jda = event.getJDA();
        final TextChannel voiceText = channelResolver.apply(jda);
        final Member member = event.getMember();
        if (SourceRole.ignoresModeration(member)) return;
        voiceText.createPermissionOverride(member).setAllow(Permission.MESSAGE_READ).queue();
    }


    private final class VoiceLeave extends AbstractListener<GuildVoiceLeaveEvent> {
        VoiceLeave() {
            super(GuildVoiceLeaveEvent.class);
        }

        @Override
        public void accept(GuildVoiceLeaveEvent event) {
            final JDA jda = event.getJDA();
            final TextChannel voiceChannel = channelResolver.apply(jda);
            final Member member = event.getMember();
            final PermissionOverride override = voiceChannel.getPermissionOverride(member);
            if (override == null) return;
            override.delete().queue();
        }


    }
}


