package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.LinkedHashMap;

public class LeaderboardCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "leaderboard",
            "Gets the rank leaderboard.",
            "(page)",
            CommandInfo.Category.GENERAL)
            .withAliases("levels", "lb")
            .withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();

        int page = 1;
        try {
            if (args.length == 1) page = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidPage(user)).build();
        }

        int cacheSize = SourceProfile.getRankCacheSize(SourceProfile.CacheType.XP);

        int maxPages = (int) Math.ceil((double) cacheSize / 10);
        if (maxPages < page || page <= 0) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidPage(user)).build();
        }


        LinkedHashMap<String, Integer> ranks = SourceProfile.getRanksByPage(SourceProfile.CacheType.XP, page);

        StringBuilder sb = new StringBuilder();
        ranks.forEach((id, rank) -> {
            try {
                SourceProfile profile = new SourceProfile(guild.getMemberById(id).getUser());
                long xp = profile.getXp();
                long level = profile.getLevel();
                User rankUser = profile.getUser();

                sb.append("**#").append(rank).append(" ").append(rankUser.getName()).append("** - Level: `")
                        .append(level).append("`, XP: `").append(xp).append("`\n\n");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });


        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        alert.setDescription(sb.toString().trim());
        alert.appendDescription("\n\nPage " + page + " of " + maxPages);

        message.getChannel().sendMessage(alert.build(source.getJda().getSelfUser(), "Rank Leaderboard")).queue();
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

}
