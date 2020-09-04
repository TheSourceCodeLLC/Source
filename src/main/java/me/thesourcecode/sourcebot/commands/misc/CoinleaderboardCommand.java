package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.LinkedHashMap;

public class CoinleaderboardCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "coinlb",
            "Gets the coin leaderboard.",
            "(page)",
            CommandInfo.Category.GENERAL)
            .withAliases("coinleaderboard", "clb")
            .withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

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

        int cacheSize = SourceProfile.getRankCacheSize(SourceProfile.CacheType.COIN);

        int maxPages = (int) Math.ceil((double) cacheSize / 10);
        if (maxPages < page || page <= 0) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidPage(user)).build();
        }


        LinkedHashMap<String, Integer> ranks = SourceProfile.getRanksByPage(SourceProfile.CacheType.COIN, page);

        StringBuilder sb = new StringBuilder();
        ranks.forEach((id, rank) -> {
            SourceProfile profile = new SourceProfile(guild.getMemberById(id).getUser());
            long coins = profile.getCoins();
            User rankUser = profile.getUser();

            sb.append("**#").append(rank).append(" ").append(rankUser.getName()).append("** - Coins: `")
                    .append(coins).append("`\n\n");

        });

        WarningAlert alert = new WarningAlert();

        alert.setDescription(sb.toString().trim());
        alert.appendDescription("\n\nPage " + page + " of " + maxPages);

        message.getChannel().sendMessage(alert.build(source.getJda().getSelfUser(), "Coin Leaderboard")).queue();
        return null;
    }


}
