package me.thesourcecode.sourcebot.listener;

import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.bson.Document;

public final class ShopListener extends AbstractListener<MessageReactionAddEvent> {

    private final DatabaseManager databaseManager;
    private final String ONE = ":one:", TWO = ":two:", THREE = ":three:", X = ":x:";
    private final Source source;

    public ShopListener(Source source) {
        super(MessageReactionAddEvent.class);
        this.databaseManager = source.getDatabaseManager();
        this.source = source;
    }

    @Override
    public void accept(MessageReactionAddEvent event) {

        if (event.getChannelType() != ChannelType.PRIVATE) return;
        if (event.getUser() == event.getJDA().getSelfUser()) return;

        PrivateChannel channel = (PrivateChannel) event.getChannel();

        Message message = channel.retrieveMessageById(event.getMessageId()).complete();
        if (message == null) return;
        if (message.getAuthor() != event.getJDA().getSelfUser()) return;

        if (message.getEmbeds().size() == 0) return;
        MessageEmbed embed = message.getEmbeds().get(0);

        if (embed.getAuthor() == null) return;

        MessageEmbed.AuthorInfo author = embed.getAuthor();
        if (!author.getName().equalsIgnoreCase("{TSC Official Discord} Shop")) return;

        MessageReaction reaction = event.getReaction();
        MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
        if (reactionEmote.isEmote()) {
            return;
        }
        String unicode = reactionEmote.getName();
        String shortcode = EmojiParser.parseToAliases(unicode, EmojiParser.FitzpatrickAction.REMOVE);

        User user = event.getUser();

        SourceProfile userProfile = new SourceProfile(user);
        long userCoins = userProfile.getCoins();

        int price = 0;
        String booster = "xp boosters";

        long expireTime = System.currentTimeMillis() + (86400000 * 14); // 14 Days
        Document cooldowns = userProfile.getCooldowns();

        SuccessAlert sAlert = new SuccessAlert();
        switch (shortcode) {
            case ONE:
                price = Utility.getShopPrices(1);
                if (userCoins < price) {
                    price = -1;
                    break;
                }

                if (userProfile.getCoinBooster() == 1.5) {
                    price = -2;
                    booster = "coin boosters";
                    break;
                }
                cooldowns.append("coinbooster", expireTime);

                userProfile.setCoinBooster(1.5);
                userProfile.setCoins(userCoins - price);
                userProfile.setCooldowns(cooldowns);


                sAlert.setDescription("You have successfully bought a 2 week 1.5x Coin Booster!");
                channel.sendMessage(sAlert.build(user)).queue();
                break;

            case TWO:
                price = Utility.getShopPrices(1);
                if (userCoins < price) {
                    price = -1;
                    break;
                }

                if (userProfile.getXpBooster() == 1.5) {
                    price = -2;
                    break;
                }

                userProfile.setXpBooster(1.5);
                userProfile.setCoins(userCoins - price);
                userProfile.setCooldowns(cooldowns);

                sAlert.setDescription("You have successfully bought a 2 week 1.5x Xp Booster!");
                channel.sendMessage(sAlert.build(user)).queue();
                break;
            /*case THREE:
                price = Utility.getShopPrices(3);

                if (userCoins < price) {
                    price = -1;
                    break;
                }

                Role pdecade = SourceRole.PREVIOUS_DECADE.resolve(event.getJDA());
                Guild guild = Utility.getGuild(source, message);
                Member member = guild.getMember(event.getUser());

                if (member.getRoles().contains(pdecade)) {
                    price = -3;
                    break;
                }

                ArrayList<String> badges = userProfile.getBadges();
                badges.add("PreviousDecade2k19");

                userProfile.setBadges(badges);
                userProfile.setCoins(userCoins - price);

                guild.addRoleToMember(member, pdecade).queue();

                sAlert.setDescription("You have successfully bought the Previous Decade Role!");
                channel.sendMessage(sAlert.build(user)).queue();
                break;*/
            case X:
                sAlert.setTitle("Success!").setDescription("You have successfully cancelled your shop purchase!");
                channel.sendMessage(sAlert.build(user)).queue();
                break;
        }

        CriticalAlert alert = new CriticalAlert();
        switch (price) {
            case -1:
                alert.setTitle("Uh Oh!").setDescription("You do not have enough coins to buy this item!");
                channel.sendMessage(alert.build(user)).queue();
                break;
            case -2:
                alert.setTitle("Uh Oh!").setDescription("You can not buy multiple " + booster + "!");
                channel.sendMessage(alert.build(user)).queue();
                break;
            /*case -3:
                alert.setTitle("Uh Oh!").setDescription("You already have the Previous Decade Role!");
                channel.sendMessage(alert.build(user)).queue();
                break;*/
        }

        message.delete().queue();
    }


}
