package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

public class ShopCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "shop",
            "Allows a user to buy items using coins.",
            "(id)",
            CommandInfo.Category.ECONOMY
    ).withUsageChannels(SourceChannel.COMMANDS).withAliases("buy");

    private final Source source;
    private final LinkedList<ShopItem> shopItemList = new LinkedList<>();

    public ShopCommand(Source source) {
        this.source = source;

        long boosterLength = 86400000 * 14; // 14 days
        shopItemList.addAll(Arrays.asList(
                new CoinBoosterItem(1, 500L, boosterLength),
                new XpBoosterItem(2, 750L, boosterLength),
                new RoleShopItem(3, 1500L)
        ));
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        MessageChannel channel = message.getChannel();

        if (args.length == 0) {
            ColoredAlert shopAlert = new ColoredAlert(SourceColor.BLUE);
            shopAlert.setAuthor("{TSC Official Discord} Shop", null, source.getJda().getSelfUser().getAvatarUrl())
                    .setDescription("To buy an item use the command `!buy <id>` (Ex: !buy 1)")
                    .addField("1. 1.5x Coin Booster", "500 Coins (Lasts 2 weeks)", false)
                    .addField("2. 1.5x Exp Booster", "750 Coins (Lasts 2 weeks)", false)
                    .addField("3. Spooktober Role", "1500 Coins (Will be in shop until November 1st)", false)
                    .addField("", "P.S. If you are looking for nicknames, just change your name by right " +
                            "clicking on the server and hitting change name, you will automatically be charged 200 coins (You will" +
                            " not be charged for resetting your nickname).", false);

            channel.sendMessage(shopAlert.build(user)).queue();
            return null;
        }


        final int itemId;
        final ShopItem shopItem;

        final SourceProfile userProfile = new SourceProfile(user);
        final long userCoins = userProfile.getCoins();


        try {
            itemId = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            return new MessageBuilder(invalidItemId().build(user)).build();
        }

        Optional<ShopItem> potentialItem = shopItemList.stream()
                .filter(item -> item.id == itemId)
                .findFirst();

        if (potentialItem.isEmpty()) {
            return new MessageBuilder(invalidItemId().build(user)).build();
        }

        shopItem = potentialItem.get();

        if (shopItem.cost > userCoins) {
            CriticalAlert errorAlert = new CriticalAlert();
            errorAlert.setTitle("Uh Oh!").setDescription("You do not have enough coins to buy this item!");

            return new MessageBuilder(errorAlert.build(user)).build();
        }

        return new MessageBuilder(shopItem.execute(user)).build();

    }

    private abstract static class ShopItem {
        final int id;
        final long cost;

        public ShopItem(int id, long cost) {
            this.id = id;
            this.cost = cost;
        }

        abstract MessageEmbed execute(User user);
    }

    private final class CoinBoosterItem extends ShopItem {

        private final long boosterLength;

        public CoinBoosterItem(int id, long cost, long boosterLength) {
            super(id, cost);
            this.boosterLength = boosterLength;
        }

        @Override
        MessageEmbed execute(User user) {
            final SourceProfile profile = new SourceProfile(user);
            final Document cooldowns = profile.getCooldowns();
            final SuccessAlert successAlert = new SuccessAlert();
            final long currentExpireTime = boosterLength + System.currentTimeMillis();

            if (profile.getCoinBooster() > 1.0) {
                return userAlreadyHasBooster("coin booster").build(user);
            }

            cooldowns.append("coinbooster", currentExpireTime);

            profile.setCoinBooster(1.5);
            profile.setCooldowns(cooldowns);
            profile.setCoins(profile.getCoins() - cost);

            successAlert.setDescription("You have successfully bought a 2 week 1.5x Coin Booster!");
            return successAlert.build(user);
        }
    }

    private final class XpBoosterItem extends ShopItem {

        private final long boosterLength;

        public XpBoosterItem(int id, long cost, long boosterLength) {
            super(id, cost);
            this.boosterLength = boosterLength;
        }

        @Override
        MessageEmbed execute(User user) {
            final SourceProfile profile = new SourceProfile(user);
            final Document cooldowns = profile.getCooldowns();
            final SuccessAlert successAlert = new SuccessAlert();
            final long currentExpireTime = boosterLength + System.currentTimeMillis();

            if (profile.getXpBooster() > 1.0) {
                return userAlreadyHasBooster("xp booster").build(user);
            }

            cooldowns.append("xpbooster", currentExpireTime);

            profile.setXpBooster(1.5);
            profile.setCooldowns(cooldowns);
            profile.setCoins(profile.getCoins() - cost);

            successAlert.setDescription("You have successfully bought a 2 week 1.5x Xp Booster!");
            return successAlert.build(user);
        }
    }

    private final class RoleShopItem extends ShopItem {

        public RoleShopItem(int id, long cost) {
            super(id, cost);
        }

        @Override
        MessageEmbed execute(User user) {
            final SourceProfile profile = new SourceProfile(user);
            final Role halloween = SourceRole.SPOOKTOBER.resolve(source.getJda());
            final Guild guild = source.getGuild();
            final Member member = guild.getMember(user);

            final CriticalAlert errorAlert = new CriticalAlert();
            final SuccessAlert successAlert = new SuccessAlert();

            errorAlert.setTitle("Uh Oh!");

            if (member == null) {
                errorAlert.setDescription("You are not in the TSC discord server!");
                return errorAlert.build(user);
            }

            if (member.getRoles().contains(halloween)) {
                errorAlert.setDescription("Uh Oh! You already have the Spooktober Role!");
                return errorAlert.build(user);
            }

            ArrayList<String> badges = profile.getBadges();
            badges.add("October2k20");

            profile.setBadges(badges);
            guild.addRoleToMember(member, halloween).queue();
            profile.setCoins(profile.getCoins() - cost);

            successAlert.setDescription("You have successfully bought the Spooktober Role!");
            return successAlert.build(user);
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

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

}