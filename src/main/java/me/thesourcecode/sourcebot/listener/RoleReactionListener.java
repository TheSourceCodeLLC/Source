package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.objects.ReactionRole;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

import java.util.*;

public class RoleReactionListener extends AbstractListener<GuildMessageReactionAddEvent> {

    private final String FREE = ":free:", MINECRAFT = "minecraft";
    private final String JAVA = "java", SPIGOT = "spigot", JDASC = "jda";
    private final String JS = "js", DJS = "djs";
    private final String HTML = "html", DATABASE = "db";

    private final long delay = 1500;
    private final Map<Member, Long> delayMap = new HashMap<>();

    private final JDA jda;
    private final Guild guild;
    private final TextChannel roleChannel;


    public RoleReactionListener(Source source) {
        super(GuildMessageReactionAddEvent.class);
        jda = source.getJda();
        guild = source.getGuild();
        roleChannel = SourceChannel.ROLES.resolve(jda);
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new RoleReactionRemoveListener().listen(listener);
    }


    @Override
    public void accept(GuildMessageReactionAddEvent event) {
        final TextChannel eventChannel = event.getChannel();
        final Member member = event.getMember();
        final User user = event.getUser();

        if (roleChannel != eventChannel) return;
        if (user.isFake() || user.isBot()) return;
        if (hasDelay(member)) return;

        final MessageReaction.ReactionEmote reactionEmote = event.getReactionEmote();
        final Role reactionRole = getReactionRole(member, reactionEmote, false);

        if (reactionRole == null) return;
        if (member.getRoles().contains(reactionRole)) return;

        Guild guild = event.getGuild();
        guild.addRoleToMember(member, reactionRole).queue();

        final long delayTime = System.currentTimeMillis() + delay;
        delayMap.put(member, delayTime);

    }

    private boolean hasDelay(Member member) {
        if (delayMap.containsKey(member)) {
            long expiration = delayMap.get(member);
            if (expiration > System.currentTimeMillis()) return true;

            delayMap.remove(member);
        }
        return false;
    }

    private Role getReactionRole(Member member, MessageReaction.ReactionEmote reactionEmote, boolean isRemoval) {
        final String reactionUnicode = reactionEmote.getName();
        final String shortcode = EmojiParser.parseToAliases(reactionUnicode, EmojiParser.FitzpatrickAction.REMOVE);

        final ReactionRole reactionRole = ReactionRole.getRRoleByShortCode(shortcode.toLowerCase());
        if (reactionRole == null) return null;

        final String restrictedRoleId = reactionRole.getRestrictedRoleId();
        if (restrictedRoleId != null && !isRemoval) {
            final Role restrictedRole = guild.getRoleById(restrictedRoleId);

            List<SourceRole> memberRoles = SourceRole.getRolesFor(member);
            List<SourceRole> exemptRoles = Arrays.asList(SourceRole.EXEMPT_FROM_ROLE_RESTRICTIONS);

            if (restrictedRole != null
                    && !member.getRoles().contains(restrictedRole)
                    && !SourceRole.ignoresModeration(member)
                    && Collections.disjoint(memberRoles, exemptRoles)) {
                return null;
            }
        }

        final String roleId = reactionRole.getRoleId();
        return guild.getRoleById(roleId);
    }


    private final class RoleReactionRemoveListener extends AbstractListener<GuildMessageReactionRemoveEvent> {

        public RoleReactionRemoveListener() {
            super(GuildMessageReactionRemoveEvent.class);
        }

        @Override
        public void accept(GuildMessageReactionRemoveEvent event) {
            final TextChannel eventChannel = event.getChannel();
            final Member member = event.getMember();
            final User user = event.getUser();

            if (roleChannel != eventChannel || member == null) return;
            if (user == null || user.isFake() || user.isBot()) return;
            if (hasDelay(member)) return;

            final MessageReaction.ReactionEmote reactionEmote = event.getReactionEmote();
            final Role reactionRole = getReactionRole(member, reactionEmote, true);

            if (reactionRole == null) return;
            if (!member.getRoles().contains(reactionRole)) return;

            final Guild guild = event.getGuild();
            guild.removeRoleFromMember(member, reactionRole).queue();

            final long delayTime = System.currentTimeMillis() + delay;
            delayMap.put(member, delayTime);
        }
    }

}
