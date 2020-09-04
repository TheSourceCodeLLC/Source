package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

public class DailyCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "daily",
            "Allows the user to claim a daily reward",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS).hasCooldown();

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        SourceProfile userProfile = new SourceProfile(user);
        Document cooldowns = userProfile.getCooldowns();

        if (args.length != 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription("Syntax: " + CommandHandler.getPrefix() + INFO.getLabel());
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        boolean lostStreak = false;
        int oldStreak = 0;
        if (cooldowns.get("dailystreak") != null) {
            long expiration = cooldowns.getLong("dailystreak");
            if (expiration < System.currentTimeMillis()) {
                oldStreak = userProfile.getDailyStreak();
                userProfile.setDailyStreak(0);
                lostStreak = true;
            }
        }

        int dailyStreak = userProfile.getDailyStreak();
        int bonus = 5;
        userProfile.addCoins(25 + (dailyStreak * bonus));
        userProfile.setDailyStreak(dailyStreak + 1);

        long newExpiration = System.currentTimeMillis() + 86400000; // 24 hours
        cooldowns.append("daily", newExpiration);
        cooldowns.append("dailystreak", newExpiration + 86400000); // 48 hours because of the 24 hour delay this would allow 24 more hours to claim the daily reward
        userProfile.setCooldowns(cooldowns);

        ColoredAlert alert = new ColoredAlert(lostStreak ? SourceColor.ORANGE : SourceColor.GREEN);
        String hasStreak = " You have received an extra " + (dailyStreak * bonus) + " coins for your daily streak of " + dailyStreak;
        String sLostStreak = "You have lost your daily streak of " + oldStreak + "!";
        alert.setDescription("You have successfully claimed your daily reward of 25 coins!" + (dailyStreak != 0 ? hasStreak : "") +
                (lostStreak ? "\n\n" + sLostStreak : ""));
        return new MessageBuilder(alert.build(user)).build();
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
