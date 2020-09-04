package me.thesourcecode.sourcebot.api.entity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum SourceController {
    DAESHAN(144285736277901312L),
    FORBIDDEN(232188192567066624L),
    JOE(644210647177101315L),
    JACK(266315409735548928L),
    GARFIELD(148214019067478017L),
    HWIGGY(399833120917946369L);

    private final long userId;

    SourceController(long userId) {
        this.userId = userId;
    }

    public static boolean isValidController(User user) {
        long userID = user.getIdLong();
        return Arrays.stream(SourceController.values())
                .map(SourceController::getUserId)
                .collect(Collectors.toList())
                .contains(userID);
    }

    public User resolve(JDA jda) {
        return jda.getUserById(userId);
    }

    public long getUserId() {
        return userId;
    }
}
