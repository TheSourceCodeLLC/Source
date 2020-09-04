package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class PayCommand extends Command {

    private static CommandInfo INFO = new CommandInfo("pay",
            "Sends the target user the specified amount of coins.",
            "<@user|id|name> <amount>",
            CommandInfo.Category.ECONOMY)
            .withUsageChannels(SourceChannel.COMMANDS)
            .withAliases("send");

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Member target = Utility.getMemberByIdentifier(source.getGuild(), args[0]);
        if (target == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("That user is not a member of TSC!");
            return new MessageBuilder(alert.build(user)).build();
        }
        User targetUser = target.getUser();
        if (targetUser == user) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not pay yourself!");
            return new MessageBuilder(alert.build(user)).build();
        }
        if (targetUser.isBot() || targetUser.isFake()) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Target!").setDescription("You may not send coins to Discord bots!");
            return new MessageBuilder(alert.build(user)).build();
        }

        try {
            SourceProfile userProfile = new SourceProfile(user);
            SourceProfile targetProfile = new SourceProfile(targetUser);

            long userCoins = userProfile.getCoins();

            long sendAmount = Long.parseLong(args[1]);
            if (sendAmount > userCoins || sendAmount <= 0) {
                CriticalAlert alert = new CriticalAlert();
                String desc = sendAmount <= 0 ? "You can not send 0 or less coins" : "You can not send more coins than you have!";
                alert.setTitle("Invalid Amount!").setDescription(desc);
                return new MessageBuilder(alert.build(user)).build();
            }

            userProfile.setCoins(userCoins - sendAmount);
            targetProfile.addCoins(sendAmount);

            String targetUserTag = targetUser.getAsTag();
            SuccessAlert sentCoins = new SuccessAlert();
            sentCoins.setTitle("Coins sent!").setDescription("You have successfully sent " + sendAmount + " coins to " + targetUserTag + "!");
            message.getChannel().sendMessage(sentCoins.build(user)).queue();
            return null;
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Invalid Long!").setDescription("`" + args[1] + "` is not a valid long!");
            return new MessageBuilder(alert.build(user)).build();
        }
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
