package me.thesourcecode.sourcebot.api.message.alerts;

import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

public class CommonAlerts extends Alert {

    public MessageEmbed noPermissions(User user) {
        setDescription("You do not have permission to use that command!");
        setColor(SourceColor.RED.asColor());
        return super.build(user, "No Permission!");
    }

    public MessageEmbed invalidUser(User user) {
        setDescription("That user is not a member of TSC!");
        setColor(SourceColor.RED.asColor());
        return super.build(user, "Invalid User!");
    }

    public MessageEmbed invalidUser(User user, Member target, String action) {
        User targetUser = target.getUser();
        JDA jda = user.getJDA();

        if (targetUser == user) {
            setDescription("You may not " + action + " yourself!");
        } else if (targetUser == jda.getSelfUser()) {
            setDescription("You may not " + action + " me :angry:!");
        } else if (SourceRole.ignoresModeration(target)) {
            setDescription("You may not " + action + " staff!");
        } else return null;
        setColor(SourceColor.RED.asColor());
        return super.build(user, "Invalid User!");
    }

    public MessageEmbed invalidBlacklistUser(User user, Member target, String category) {
        User targetUser = target.getUser();
        JDA jda = user.getJDA();

        List<SourceRole> targetRoles = SourceRole.getRolesFor(target);

        if (targetRoles.contains(SourceRole.BLACKLIST)) {
            setDescription("You may not blacklist this user from the " + category + " because they already are blacklisted from them!");
        } else if (targetRoles.contains(SourceRole.DEV) || SourceRole.ignoresModeration(target)) {
            setDescription("You may not blacklist developers or staff!");
        } else if (targetUser == jda.getSelfUser()) {
            setDescription("You may not blacklist me :angry:!");
        } else return null;

        setColor(SourceColor.RED.asColor());
        return super.build(user, "Invalid User!");
    }

    public MessageEmbed invalidPage(User user) {
        setDescription("You did not enter a valid page number!");
        setColor(SourceColor.RED.asColor());
        return super.build(user, "Invalid Page!");
    }

    public MessageEmbed invalidDuration(User user, String type) {
        setDescription("You did not enter a valid duration " + type);
        setColor(SourceColor.RED.asColor());
        return super.build(user, "Uh Oh!");
    }

    public MessageEmbed invalidTag(User user) {
        setDescription("There is no tag by that name! See `!tag list` for all of the current tags");
        setColor(SourceColor.RED.asColor());
        return super.build(user, "Uh Oh!");
    }

    public MessageEmbed blacklistEmbed(User user, User target, String category, String duration, String reason, long caseId) {
        setDescription("**Blacklisted By:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                "**Blacklisted User:** " + target.getAsTag() + " (" + target.getId() + ")\n" +
                "**Duration:** " + duration + "\n" +
                "**Reason:** " + reason);
        setColor(SourceColor.ORANGE.asColor());
        return super.build(target, category + " Blacklist | Id: " + caseId);
    }


}
