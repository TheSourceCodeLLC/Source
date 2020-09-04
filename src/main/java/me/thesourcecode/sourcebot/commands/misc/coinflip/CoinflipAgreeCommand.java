package me.thesourcecode.sourcebot.commands.misc.coinflip;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CoinflipAgreeCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "accept",
            "Allows a user to accept a coinflip challenge.",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS);


    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.getGuild();
        Member member = guild.getMember(user);

        HashMap<User, ArrayList<Object>> coinflip = CoinflipCommand.coinflip;
        if (!coinflip.containsKey(user)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You are currently in no coinflips!");
            return new MessageBuilder(alert.build(user)).build();
        }

        ArrayList<Object> info = coinflip.get(user);
        User target = (User) info.get(0);
        long coinBetAmount = (long) info.get(1);

        SourceProfile userProfile = new SourceProfile(user);
        SourceProfile targetProfile = new SourceProfile(target);

        long userCoins = userProfile.getCoins();
        long targetCoins = targetProfile.getCoins();

        if (targetCoins < coinBetAmount || userCoins < coinBetAmount) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You are or your opponent does not have enough coins!");
            return new MessageBuilder(alert.build(user)).build();
        }

        Random random = new Random();
        int chance = random.nextInt(100 - 1 + 1) + 1;

        String tTag = target.getAsTag();
        String uTag = user.getAsTag();

        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        alert.setAuthor("Coinflip", null, source.getJda().getSelfUser().getAvatarUrl());
        String aCoin = coinBetAmount == 1 ? " coin " : " coins ";

        alert.setDescription("\n" + uTag + ", you are heads.\n" + tTag + ", you are tails.");
        if (chance >= 50) {
            alert.appendDescription("\n\nHeads! " + uTag + " has won the coinflip! \n" + coinBetAmount + aCoin + "have been added to your balance!");

            userProfile.addCoins(coinBetAmount * 2); // Times by 2 to add back the coins taken from them at original command
        } else {
            alert.appendDescription("\n\nTails! " + tTag + " has won the coinflip! \n" + coinBetAmount + aCoin + "have been added to your balance");

            targetProfile.addCoins(coinBetAmount * 2); // Times by 2 to add back the coins taken from them at original command
        }

        message.getChannel().sendMessage(alert.build(source.getJda().getSelfUser(), "Coinflip")).queue();
        Document userCooldowns = userProfile.getCooldowns();
        Document targetCooldowns = targetProfile.getCooldowns();

        long expire = System.currentTimeMillis() + (5 * (60 * 1000));
        if (!SourceRole.getRolesFor(member).contains(SourceRole.ADMIN))
            userCooldowns.append("coinflip", expire);
        if (!SourceRole.getRolesFor(guild.getMember(target)).contains(SourceRole.ADMIN))
            targetCooldowns.append("coinflip", expire);

        coinflip.remove(user);
        return null;
    }

}
