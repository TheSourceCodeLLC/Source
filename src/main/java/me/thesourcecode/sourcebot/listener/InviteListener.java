package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class InviteListener extends AbstractListener<GuildMessageReceivedEvent> {

    public InviteListener() {
        super(GuildMessageReceivedEvent.class);
    }

    public void accept(GuildMessageReceivedEvent event) {
        checkAdvertising(event.getMessage());
    }

    private void checkAdvertising(Message message) {
        JDA jda = message.getJDA();
        Member member = message.getMember();
        TextChannel channel = message.getTextChannel();
        List<String> messageInvites = message.getInvites();
        List<String> invites = new ArrayList<>(messageInvites);
        List<String> guildInvites = message.getGuild().retrieveInvites().complete()
                .stream().map(Invite::getCode).collect(Collectors.toList());

        try {
            guildInvites.add(message.getGuild().retrieveVanityUrl().complete());
        } catch (IllegalStateException ignored) {
        }


        invites.removeIf(guildInvites::contains);
        if (invites.size() == 0) {
            return;
        }
        int resolved;
        try {
            resolved = (int) invites.stream().map(code -> Invite.resolve(jda, code).complete()).filter(Objects::nonNull).count();
        } catch (Exception e) {
            return;
        }
        if (resolved == 0) {
            return;
        }
        TextChannel devChannel = SourceChannel.DEVELOPERS.resolve(jda);
        TextChannel devLeadChannel = SourceChannel.DEV_LEADERS.resolve(jda);
        if (channel.equals(devChannel) || channel.equals(devLeadChannel)) {
            //Ignore invites in dev chat & dev leaders chat
            return;
        }
        if (SourceRole.ignoresModeration(member)) {
            return;
        }

        //TODO: This was a valid invite to a non-TSC Guild, by a non-ignored user in a non-ignored channel
        message.delete().queue();

        TextChannel reports = SourceChannel.REPORTS.resolve(jda);
        User user = member.getUser();
        WarningAlert alert = new WarningAlert();

        String format = "**User:** %s (%s)\n**Channel:** %s (%s)";
        alert.setDescription(String.format(format, user.getAsTag(), user.getId(), channel.getName(), channel.getId()));
        alert.addField("Message", message.getContentRaw(), false);
        Message newReport = reports.sendMessage(alert.build(user, "Potential Advertisement")).complete();
        newReport.addReaction(EmojiParser.parseToUnicode(":white_check_mark:")).complete();
        newReport.addReaction(EmojiParser.parseToUnicode(":x:")).complete();

        CriticalAlert cAlert = new CriticalAlert();
        cAlert.setDescription("Please do not send discord invites!");
        channel.sendMessage(cAlert.build(user)).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new InviteListener.InviteEditListener().listen(listener);
    }

    private final class InviteEditListener extends AbstractListener<GuildMessageUpdateEvent> {

        InviteEditListener() {
            super(GuildMessageUpdateEvent.class);
        }

        @Override
        public void accept(GuildMessageUpdateEvent event) {
            checkAdvertising(event.getMessage());
        }
    }


}
