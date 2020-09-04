package me.thesourcecode.sourcebot.api.message.alerts;

import me.thesourcecode.sourcebot.BotMain;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;

import java.time.ZonedDateTime;

public class Alert extends EmbedBuilder {

    public MessageEmbed build(User user, String authorOverride) {
        if (user == null) return super.build();
        String name = user.getName();
        String icon = user.getEffectiveAvatarUrl();
        JDA jda = user.getJDA();
        String author = (authorOverride == null || authorOverride.isEmpty()) ? name : authorOverride;
        return build(jda, author, icon);
    }

    public MessageEmbed build(User user) {
        return build(user, null);
    }

    public MessageEmbed build(JDA jda, String author, String icon) {
        if (author == null) return super.build();
        SelfUser selfUser = jda.getSelfUser();
        String selfIcon = selfUser.getEffectiveAvatarUrl();
        if (icon == null) {
            icon = selfIcon;
        }
        setAuthor(author, "https://sourcebot.net", icon);
        String footer = "TheSourceCode • https://sourcebot.net";
        ZonedDateTime now = ZonedDateTime.now(BotMain.TIME_ZONE);
        setFooter(footer, selfUser.getEffectiveAvatarUrl());
        setTimestamp(now);
        return super.build();
    }
}
