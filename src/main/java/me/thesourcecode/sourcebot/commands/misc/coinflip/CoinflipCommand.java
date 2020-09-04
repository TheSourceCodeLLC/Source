package me.thesourcecode.sourcebot.commands.misc.coinflip;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class CoinflipCommand extends Command {

    public static final HashMap<User, ArrayList<Object>> coinflip = new HashMap<>();
    private final CommandInfo INFO = new CommandInfo(
            "coinflip",
            "Allows a user to challenge another user to a coinflip for coins.",
            "<@user|id|name> <coin amount>",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS)
            .withAliases("cf")
            .hasCooldown();

    public CoinflipCommand() {
        registerSubcommand(new CoinflipAgreeCommand());
        registerSubcommand(new CoinflipDenyCommand());
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Member target = Utility.getMemberByIdentifier(message.getGuild(), args[0]);
        if (target == null) {
            CommonAlerts alert = new CommonAlerts();
            return new MessageBuilder(alert.invalidUser(user)).build();
        }
        User targetUser = target.getUser();

        for (User key : coinflip.keySet()) {
            ArrayList<Object> info = coinflip.get(key);
            User sent = (User) info.get(0);
            if (targetUser == key || targetUser == sent) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Invalid Target!").setDescription("You can not challenge someone to a coinflip who has/has been challenged someone to a coinflip in the last minute!");
                return new MessageBuilder(alert.build(user)).build();
            } else if (user == key || user == sent) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Invalid Target!").setDescription("You can not challenge someone to a coinflip as you have, or have been, challenged someone to a coinflip in the last minute!");
                return new MessageBuilder(alert.build(user)).build();
            }
        }


        if (targetUser == user) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not challenge yourself to a coinflip!");
            return new MessageBuilder(alert.build(user)).build();
        } else if (targetUser.isBot()) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not challenge bots to a coinflip!");
            return new MessageBuilder(alert.build(user)).build();
        }
        if (Utility.checkCooldown(message, INFO.getLabel(), true)) {
            return null;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid coin amount!");
            return new MessageBuilder(alert.build(user)).build();
        }
        if (amount <= 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not bet 0 or less coins!");
            return new MessageBuilder(alert.build(user)).build();
        }

        DatabaseManager dbManager = source.getDatabaseManager();
        SourceProfile userProfile = new SourceProfile(user);
        SourceProfile targetProfile = new SourceProfile(targetUser);

        long userCoins = userProfile.getCoins();
        long targetCoins = targetProfile.getCoins();

        if (userCoins < amount || targetCoins < amount) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not bet more coins than you or the other user has!");
            return new MessageBuilder(alert.build(user)).build();
        }

        long expirationTime = System.currentTimeMillis() + (60 * 1000);
        ArrayList<Object> info = new ArrayList<>();
        info.add(user);
        info.add(amount);
        info.add(expirationTime);
        coinflip.put(targetUser, info);

        String uTag = user.getAsTag();
        String tTag = targetUser.getAsTag();
        ColoredAlert cAlert = new ColoredAlert(SourceColor.BLUE);
        String aCoin = amount == 1 ? " coin!" : " coins!";
        cAlert.setDescription(tTag + ", you have been challenged to a coinflip by " + uTag + " for " + amount + aCoin +
                "\n\nType `!coinflip accept` to accept or `!coinflip deny` to deny");
        message.getChannel().sendMessage(targetUser.getAsMention()).queue();
        message.getChannel().sendMessage(cAlert.build(targetUser)).queue();

        userProfile.setCoins(userCoins - amount);
        targetProfile.setCoins(targetCoins - amount);

        Timer timer = new Timer();

        source.getExecutorService().scheduleAtFixedRate(() -> {
            if (coinflip.containsKey(targetUser)) {
                ArrayList<Object> coinInfo = coinflip.get(targetUser);
                long expireTime = (Long) coinInfo.get(2);
                if (expireTime < System.currentTimeMillis()) {

                    CriticalAlert critAlert = new CriticalAlert();
                    critAlert.setDescription("The coinflip between " + uTag + " and " + tTag + " has expired!");

                    message.getChannel().sendMessage(user.getAsMention() + " " + target.getAsMention()).queue();
                    message.getChannel().sendMessage(critAlert.build(source.getJda().getSelfUser())).queue();

                    userProfile.refreshProfile();
                    targetProfile.refreshProfile();

                    userProfile.addCoins(amount);
                    targetProfile.addCoins(amount);

                    coinflip.remove(targetUser);
                    timer.cancel();
                }
            } else timer.cancel();
        }, 0, 1, TimeUnit.MINUTES);


        return null;
    }
}
