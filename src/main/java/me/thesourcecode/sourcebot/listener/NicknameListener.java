package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class NicknameListener extends AbstractListener<GuildMemberUpdateNicknameEvent> {

    private final Source source;

    private final HashMap<User, User> nickBypass = new HashMap<>();
    private final long cost = 200;

    private final HashMap<String, User> nicknameConfirmation = new HashMap<>();
    private final String CHECK = ":white_check_mark:", X = ":x:";

    public NicknameListener(Source source) {
        super(GuildMemberUpdateNicknameEvent.class);
        this.source = source;
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new NicknameReactionListener().listen(listener);
    }

    @Override
    public void accept(GuildMemberUpdateNicknameEvent event) {
        JDA jda = event.getJDA();
        String prevNick = event.getOldNickname();
        String newNick = event.getNewNickname();
        User user = event.getUser();

        List<SourceRole> memberRoles = SourceRole.getRolesFor(event.getMember());
        if (memberRoles.contains(SourceRole.DONOR) || memberRoles.contains(SourceRole.NITRO_BOOSTER)
                || SourceRole.ignoresModeration(event.getMember())) {
            return;
        }

        AuditLogEntry auditLog = event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).complete().get(0);
        User foundUser = auditLog.getUser();
        if (foundUser != user) {
            return;
        }
        if (nickBypass.containsKey(user)) {
            User foundBypass = nickBypass.get(user);
            if (foundBypass == jda.getSelfUser() && foundUser == jda.getSelfUser()) {
                nickBypass.remove(user);
                return;
            } else {
                nickBypass.remove(user);
            }
        }
        if (newNick == null) {
            SuccessAlert alert = new SuccessAlert();
            alert.setTitle("Nickname Changed!").setDescription("You have successfully removed your nickname!");
            dmUser(jda, user, alert.build(user));
            return;
        }

        SourceProfile userProfile = new SourceProfile(user);

        long userCoins = userProfile.getCoins();

        if (userCoins < cost) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Balance!").setDescription("You cannot afford a nickname change!");
            dmUser(jda, user, alert.build(user));
            event.getGuild().modifyNickname(event.getMember(), prevNick).queue();
            nickBypass.put(user, jda.getSelfUser());
            return;
        }
        ColoredAlert confirmEmbed = new ColoredAlert(SourceColor.ORANGE);

        String alertDesc = "Please confirm, within the next 60 seconds, that you want to spend `%d` coins to" +
                " change your nickname to `%s` by reacting with \n\n%s for Yes \n%s for No";

        confirmEmbed.setTitle("Confirmation Message").setDescription(String.format(alertDesc, cost, newNick, CHECK, X));
        Message confirmationMessage = dmUser(jda, user, confirmEmbed.build(user));

        String checkUnicode = EmojiParser.parseToUnicode(CHECK);
        String xUnicode = EmojiParser.parseToUnicode(X);
        Stream.of(
                checkUnicode,
                xUnicode
        ).map(confirmationMessage::addReaction).forEach(RestAction::queue);

        nicknameConfirmation.put(confirmationMessage.getId(), user);

        userCoins -= cost;

        userProfile.setOldNickname(prevNick == null ? "N/A" : prevNick);
        userProfile.setCoins(userCoins);

        source.getExecutorService().schedule(() -> {
            if (!nicknameConfirmation.containsKey(confirmationMessage.getId())) return;

            userProfile.refreshProfile();
            userProfile.addCoins(cost);
            userProfile.setOldNickname("N/A");

            event.getGuild().modifyNickname(event.getMember(), prevNick).queue();
            nicknameConfirmation.remove(confirmationMessage.getId(), user);
            nickBypass.put(user, event.getJDA().getSelfUser());

            CriticalAlert alert = new CriticalAlert();
            alert.setDescription("Uh Oh!").setDescription("You did not react to the confirmation message in time, so your coins have been refunded to you!");

            dmUser(jda, user, alert.build(user));
        }, 60, TimeUnit.SECONDS);
    }

    private Message dmUser(JDA jda, User user, MessageEmbed build) {
        try {
            PrivateChannel dm = user.openPrivateChannel().complete();
            return dm.sendMessage(build).complete();
        } catch (Exception e) {
            TextChannel commands = SourceChannel.COMMANDS.resolve(jda);
            MessageBuilder builder = new MessageBuilder(user.getAsMention());
            builder.setEmbed(build);
            return commands.sendMessage(builder.build()).complete();
        }
    }

    private final class NicknameReactionListener extends AbstractListener<MessageReactionAddEvent> {
        NicknameReactionListener() {
            super(MessageReactionAddEvent.class);
        }

        @Override
        public void accept(MessageReactionAddEvent event) {
            String messageId = event.getMessageId();
            if (!nicknameConfirmation.containsKey(messageId)) return;

            User reactor = event.getUser();
            User requester = nicknameConfirmation.get(messageId);

            if (requester != reactor) return;

            MessageReaction reaction = event.getReaction();
            MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
            if (reactionEmote.isEmote()) {
                return;
            }

            String unicode = reactionEmote.getName();
            String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);

            Member member = source.getGuild().getMember(reactor);
            SourceProfile userProfile = new SourceProfile(reactor);

            SuccessAlert successAlert = new SuccessAlert();
            switch (shortcode) {
                case CHECK:
                    String newNick = member.getNickname();
                    nickBypass.put(reactor, reactor);

                    successAlert.setTitle("Nickname Changed!")
                            .setDescription("Your nickname has been set to '" + newNick + "'!\n" +
                                    cost + " coins have been subtracted from your balance");
                    break;
                case X:
                    String prevNick = userProfile.getOldNickname();
                    prevNick = prevNick.equalsIgnoreCase("N/A") ? "" : prevNick;

                    member.modifyNickname(prevNick).queue();
                    userProfile.addCoins(cost);
                    nickBypass.put(reactor, event.getJDA().getSelfUser());

                    successAlert.setTitle("Nickname Changed Cancelled!")
                            .setDescription("You have successfully cancelled you nickname change!");
                    break;
                default:
                    return;
            }

            boolean isDM = event.getChannelType() == ChannelType.PRIVATE;
            MessageChannel channel = event.getChannel();

            MessageBuilder messageBuilder = new MessageBuilder();
            if (!isDM) {
                TextChannel commands = SourceChannel.COMMANDS.resolve(event.getJDA());
                Message message = commands.retrieveMessageById(messageId).complete();

                message.delete().queue();

                messageBuilder.append(reactor.getAsMention());
            }

            messageBuilder.setEmbed(successAlert.build(reactor));

            channel.sendMessage(messageBuilder.build()).queue(m -> {
                if (!isDM) m.delete().queueAfter(15, TimeUnit.SECONDS);
            });

            nicknameConfirmation.remove(messageId, requester);
        }
    }
}
