package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GambleCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "gamble",
            "Allows a user to gamble coins",
            "<coin amount>",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS)
            .withAliases("bet", "g")
            .hasCooldown();

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();
        Member member = Utility.getMemberByIdentifier(guild, user.getId());
        if (member == null) return null;


        SourceProfile userProfile = new SourceProfile(user);
        Document cooldowns = userProfile.getCooldowns();

        long amount;
        try {
            amount = Long.parseLong(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid coin amount!");
            return new MessageBuilder(alert.build(user)).build();
        }

        long coins = userProfile.getCoins();

        List<SourceRole> sMemberRoles = SourceRole.getRolesFor(member);
        SourceRole[] higherCap = {SourceRole.VIP, SourceRole.MVP, SourceRole.DONOR, SourceRole.CONTENT_CREATOR,
                SourceRole.ADMIN, SourceRole.OWNER};

        if (amount <= 0 || (Collections.disjoint(sMemberRoles, Arrays.asList(higherCap)) && amount > 300)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not gamble " + (amount <= 0 ? "zero or less coins "
                    : "more than 300 coins") + "!");
            return new MessageBuilder(alert.build(user)).build();
        } else if (!Collections.disjoint(sMemberRoles, Arrays.asList(higherCap)) && amount > 500) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not gamble more than 500 coins!");
            return new MessageBuilder(alert.build(user)).build();
        } else if (amount > coins) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not gamble more coins than you have!");
            return new MessageBuilder(alert.build(user)).build();
        }


        Random r = new Random();
        int low = 1;
        int high = 100;
        int result = r.nextInt(high - low) + low;

        boolean won = false;
        double coinbooster = userProfile.getCoinBooster();
        if (result > 40) {
            won = true;
            coins = coins + (int) Math.round(amount * coinbooster);
        } else coins = coins - amount;

        ColoredAlert alert = new ColoredAlert(won ? SourceColor.GREEN : SourceColor.RED);
        String aCoin = amount == 1 ? " coin" : " coins";
        alert.setDescription(won ? "You have won " + amount + aCoin + " and received your original bet back!" :
                "You have lost " + amount + aCoin + "!");

        aCoin = coins == 1 ? " coin" : " coins";
        alert.appendDescription("\nYou now have " + coins + aCoin + ".");
        if (won && coinbooster > 1) {
            long boosterAmount = Math.round(amount * coinbooster);
            alert.appendDescription("\n\nYou have received an extra " + (boosterAmount - amount) + " coins due to your " +
                    coinbooster + "x coin booster!");
        }


        userProfile.setCoins(coins);
        long newExpiration = System.currentTimeMillis() + (5 * (60 * 1000)); // 5 minutes


        cooldowns.append("gamble", newExpiration);
        userProfile.setCooldowns(cooldowns);

        message.getChannel().sendMessage(alert.build(user)).queue();
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
