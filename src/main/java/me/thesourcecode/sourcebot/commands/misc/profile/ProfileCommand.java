package me.thesourcecode.sourcebot.commands.misc.profile;

import me.thesourcecode.sourcebot.BotMain;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProfileCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "profile",
            "Allows a user to view their or the specified user's profile.",
            "(set|@user|id|name)",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS)
            .withAliases("coins", "bal", "rank");

    public ProfileCommand() {
        registerSubcommand(new ProfileSetComand());
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User target = message.getAuthor();
        Guild guild = source.getGuild();
        Member member = Utility.getMemberByIdentifier(guild, target.getId());

        if (args.length >= 1) {
            member = Utility.getMemberByIdentifier(guild, args[0]);
            if (member == null) {
                CommonAlerts alert = new CommonAlerts();
                return new MessageBuilder(alert.invalidUser(message.getAuthor())).build();
            }
            target = member.getUser();
            if (target.isBot() || target.isFake()) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Invalid Target!").setDescription("You may not view the profile of Discord bots!");
                return new MessageBuilder(alert.build(message.getAuthor())).build();
            }
        }


        SourceProfile userProfile = new SourceProfile(target);
        userProfile.balanceLevel();
        String nickname = member.getEffectiveName();

        long level = userProfile.getLevel();
        int xpRank = userProfile.getXpRank();
        long xp = userProfile.getXp();
        int coinRank = userProfile.getCoinRank();
        long coins = userProfile.getCoins();

        double xpbooster = userProfile.getXpBooster();
        double coinbooster = userProfile.getCoinBooster();
        int dailystreak = userProfile.getDailyStreak();

        boolean coinNotifications = userProfile.getCoinMessageToggle();
        String coinNotificationsString = coinNotifications ? "Activated" : "Deactivated";

        long previousLevels = Utility.getAllPreviousLevels(level);
        long neededXp = Utility.getXpFormula(level + 1);
        long rankupXp = level == 0 ? xp : xp - previousLevels;

        String bio = userProfile.getBio();
        String github = userProfile.getGithub();

        String xpExpiration = Utility.getCooldown(member.getUser(), "xpbooster");
        String coinExpiration = Utility.getCooldown(member.getUser(), "coinbooster");

        long vipLevel = Utility.vipLevel;
        long vipPlusLevel = Utility.mvpLevel;
        String nextRole = level >= vipPlusLevel ? "You already have all of the acquirable ranks!" : "%s needed to get %s";
        if (level < vipLevel) {
            long levelsNeeded = vipLevel - level;
            nextRole = String.format(nextRole, (levelsNeeded == 1 ? "1 more level" : levelsNeeded + " more levels"), "VIP");
        } else if (level < vipPlusLevel) {
            long levelsNeeded = vipPlusLevel - level;
            nextRole = String.format(nextRole, (levelsNeeded == 1 ? "1 more level" : levelsNeeded + " more levels"), "MVP");
        }

        String description = "**Nickname:** %s\n" +
                "**Level:** %s (**Rank:** %s)\n" +
                "**XP:** %s (**To Level Up:** %s/%s)\n" +
                "**Coins:** %s (**Rank:** %s)\n" +
                "**Daily Streak:** %s\n" +
                "**Coin Notifications:** %s\n" +
                "**Next Role:** %s\n";


        ColoredAlert coloredAlert = new ColoredAlert(SourceColor.BLUE);

        coloredAlert.setThumbnail(target.getEffectiveAvatarUrl())
                .setDescription(String.format(description, nickname, level, xpRank, xp, rankupXp, neededXp,
                        coins, coinRank, dailystreak, coinNotificationsString, nextRole));

        String url = "https://sourcebot.net/user/" + target.getId();
        coloredAlert.setAuthor(target.getName() + "'s Profile:", url, target.getEffectiveAvatarUrl());

        String footer = "TheSourceCode • https://sourcebot.net";
        coloredAlert.setFooter(footer, target.getJDA().getSelfUser().getEffectiveAvatarUrl());

        ZonedDateTime now = ZonedDateTime.now(BotMain.TIME_ZONE);
        coloredAlert.setTimestamp(now);

        List<String> badgeList = userProfile.getBadges();

        if (badgeList != null) {
            StringBuilder badgeBuilder = new StringBuilder();
            badgeList.forEach(badge -> {
                switch (badge.toLowerCase()) {
                    case "october2k19":
                        badgeBuilder.append(":ghost: - 2019 Spooktober\n");
                        break;
                    case "october2k18":
                        badgeBuilder.append(":jack_o_lantern: - 2018 Spooktober\n");
                        break;
                    case "december2k18":
                        badgeBuilder.append(":santa::skin-tone-2: - 2018 Christmas\n");
                        break;
                    case "december2k19":
                        badgeBuilder.append(":gift: - 2019 Christmas\n");
                        break;
                    case "previousdecade2k19":
                        badgeBuilder.append(":clock: - Here before 2020\n");
                }
            });

            String badges = badgeBuilder.toString().trim();
            if (!badges.isBlank()) {
                coloredAlert.addField("Badges", badges.trim(), false);
            }
        }

        if (xpbooster > 1 || coinbooster > 1) {
            String boosters = "";
            ArrayList<Object> values = new ArrayList<>();
            if (xpbooster > 1) {
                boosters = "**XP:** %sx (**Expires In:** %s)\n";
                xpExpiration = xpExpiration == null ? "Never" : xpExpiration;
                values.add(xpbooster);
                values.add(xpExpiration);
            }
            if (coinbooster > 1) {
                boosters += "**Coins:** %sx (**Expires In:** %s)";
                coinExpiration = coinExpiration == null ? "Never" : coinExpiration;
                values.add(coinbooster);
                values.add(coinExpiration);
            }
            coloredAlert.addField("Boosters", String.format(boosters, values.toArray()), false);
        }


        ZonedDateTime zonedJoin = member.getTimeJoined().atZoneSameInstant(BotMain.TIME_ZONE);
        ZonedDateTime zonedCreation = target.getTimeCreated().atZoneSameInstant(BotMain.TIME_ZONE);

        String joinDate = zonedJoin.format(BotMain.DATE_FORMAT);
        String joinTime = zonedJoin.format(BotMain.TIME_FORMAT);

        String creationDate = zonedCreation.format(BotMain.DATE_FORMAT);
        String creationTime = zonedCreation.format(BotMain.TIME_FORMAT);

        String join = joinDate + " " + joinTime;
        if (target.getId().equals("232188192567066624")) join = "Mar 18, 2017 at 05:05 PM";

        String creation = creationDate + " " + creationTime;
        String dateFormat = "" +
                "**Creation Date:** %s\n" +
                "**Join Date:** %s";

        coloredAlert.addField("Account Information", String.format(dateFormat, creation, join), false);

        if (!bio.equals("")) {
            coloredAlert.addField("Bio:", bio, true);
        }
        if (!github.equals("")) {
            coloredAlert.addField("GitHub:", github, true);
        }

        message.getChannel().sendMessage(coloredAlert.build(null)).queue();

        return null;
    }


}
