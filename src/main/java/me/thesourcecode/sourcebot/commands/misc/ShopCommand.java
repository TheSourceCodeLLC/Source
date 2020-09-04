package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

public class ShopCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "shop",
            "Allows a user to buy items using coins.",
            "(id)",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS).withAliases("buy");

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        MessageChannel channel = message.getChannel();

        if (args.length == 0) {
            ColoredAlert shopAlert = new ColoredAlert(SourceColor.BLUE);
            shopAlert.setAuthor("{TSC Official Discord} Shop", null, source.getJda().getSelfUser().getAvatarUrl())
                    .setDescription("To buy an item use the command `!buy <id>` (Ex: !buy 1)")
                    .addField("1. 1.5x Coin Booster", "200 Coins (Lasts 2 weeks)", false)
                    .addField("2. 1.5x Exp Booster", "250 Coins (Lasts 2 weeks)", false)
                    //.addField("3. Previous Decade Role", "400 Coins (Will be in shop until January 1st)", false)
                    .addField("", "P.S. If you are looking for nicknames, just change your name by right " +
                            "clicking on the server and hitting change name, you will automatically be charged 200 coins (You will" +
                            " not be charged for resetting your nickname).", false);

            channel.sendMessage(shopAlert.build(user)).queue();
            return null;
        } else {
            int itemId;
            try {
                itemId = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                return new MessageBuilder(invalidItemId().build(user)).build();
            }

            if (itemId < 1 || itemId > 2) {
                return new MessageBuilder(invalidItemId().build(user)).build();
            }

            SourceProfile userProfile = new SourceProfile(user);
            long userCoins = userProfile.getCoins();

            long itemPrice = getItemPrice(itemId);

            CriticalAlert errorAlert = new CriticalAlert();
            ColoredAlert successAlert = new SuccessAlert();

            if (itemPrice > userCoins) {
                errorAlert.setTitle("Uh Oh!").setDescription("You do not have enough coins to buy this item!");
                return new MessageBuilder(errorAlert.build(user)).build();
            }

            Document cooldowns = userProfile.getCooldowns();
            long boostExpireTime = System.currentTimeMillis() + (86400000 * 14); // 14 Days

            switch (itemId) {
                case 1:
                    if (userProfile.getCoinBooster() > 1.0) {
                        return new MessageBuilder(userAlreadyHasBooster("coin booster").build(user)).build();
                    }

                    cooldowns.append("coinbooster", boostExpireTime);

                    userProfile.setCoinBooster(1.5);
                    userProfile.setCooldowns(cooldowns);

                    successAlert.setDescription("You have successfully bought a 2 week 1.5x Coin Booster!");
                    break;
                case 2:
                    if (userProfile.getXpBooster() > 1.0) {
                        return new MessageBuilder(userAlreadyHasBooster("xp booster").build(user)).build();
                    }

                    cooldowns.append("xpbooster", boostExpireTime);

                    userProfile.setXpBooster(1.5);
                    userProfile.setCooldowns(cooldowns);

                    successAlert.setDescription("You have successfully bought a 2 week 1.5x Xp Booster!");
                    break;
                /*case 3:
                    Role pdecade = SourceRole.PREVIOUS_DECADE.resolve(source.getJda());
                    Guild guild = source.getGuild();
                    Member member = guild.getMember(user);

                    if (member.getRoles().contains(pdecade)) {
                        errorAlert.setTitle("Uh Oh!").setDescription("Uh Oh! You already have the Previous Decade Role!");
                        break;
                    }

                    ArrayList<String> badges = userProfile.getBadges();
                    badges.add("PreviousDecade2k19");

                    userProfile.setBadges(badges);
                    guild.addRoleToMember(member, pdecade).queue();

                    successAlert.setDescription("You have successfully bought the Previous Decade Role!");
                    break;*/
            }

            userProfile.setCoins(userCoins - itemPrice);
            return new MessageBuilder(successAlert.build(user)).build();
        }

    }

    private CriticalAlert invalidItemId() {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Uh Oh!").setDescription("Uh Oh! You did not enter a valid item id!");
        return alert;
    }

    private CriticalAlert userAlreadyHasBooster(String boosterType) {
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Uh Oh!").setDescription("You can not buy multiple " + boosterType + "s!");
        return alert;
    }

    private long getItemPrice(int itemId) {
        switch (itemId) {
            case 1:
                return 200;
            case 2:
                return 250;
            case 3:
                return 400;
        }
        return 0;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

}