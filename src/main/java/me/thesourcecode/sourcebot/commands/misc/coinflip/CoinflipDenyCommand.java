package me.thesourcecode.sourcebot.commands.misc.coinflip;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.HashMap;

public class CoinflipDenyCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "deny",
            "Allows a user to decline a coinflip challenge.",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS).withAliases("decline");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        HashMap<User, ArrayList<Object>> coinflip = CoinflipCommand.coinflip;
        if (!coinflip.containsKey(user)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You are currently in no coinflips!");
            return new MessageBuilder(alert.build(user)).build();
        }

        ArrayList<Object> coinflipInfo = coinflip.get(user);
        User targetUser = (User) coinflipInfo.get(0);
        long coinBetAmount = (long) coinflipInfo.get(1);
        String tTag = targetUser.getAsTag();

        SourceProfile userProfile = new SourceProfile(user);
        SourceProfile targetProfile = new SourceProfile(targetUser);

        userProfile.addCoins(coinBetAmount);
        targetProfile.addCoins(coinBetAmount);

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully denied " + tTag + "'s coinflip challenge!");
        coinflip.remove(user);
        return new MessageBuilder(alert.build(user)).build();
    }
}
