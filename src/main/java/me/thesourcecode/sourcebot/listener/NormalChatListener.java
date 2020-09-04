package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceController;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceMute;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class NormalChatListener extends AbstractListener<GuildMessageReceivedEvent> {

    private final HashMap<User, Long> delays;
    private final String[] blockedFileExtensions = {
            "java", "js", "ts", "cpp", "c++", "c", "cs", "h", "h++", "hh", "b", "bf", "html", "css", "htm", "xml",
            "php", "py", "swift", "tsx", "go", "dart", "sql", "rb", "ruby", "ru", "r", "perl", "pl", "kt", "kts", "ktm",
            "rs", "scala", "ex", "exs", "hs", "hsc"
    };
    private final int mentionThreshold = 4;

    public NormalChatListener() {
        super(GuildMessageReceivedEvent.class);
        this.delays = new HashMap<>();
    }

    @Override
    public void accept(GuildMessageReceivedEvent event) {
        User user = event.getAuthor();
        if (user.isBot() || user.isFake()) return;

        SourceProfile profile = new SourceProfile(user);

        TextChannel channel = event.getChannel();
        Message message = event.getMessage();

        if (
                message.getChannel() == SourceChannel.AGREE.resolve(event.getJDA())
                        && !SourceController.isValidController(user)
        ) {
            final String raw = message.getContentRaw();
            if (!raw.startsWith("!agree") && !raw.startsWith("!verify")) {
                user.openPrivateChannel().queue(dm ->
                        dm.sendMessage("**Read** the messages in <#491387395825074199> to learn how to verify _properly_.").queue()
                );
                message.delete().queue();
            }
        }

        if (containsBlockedFiles(message)) return;
        if (Utility.isCommand(message)) return;

        if (!delays.containsKey(user)) {
            randomCoinXp(channel, user, profile);
            long delay = System.currentTimeMillis() + (60 * 1000);
            delays.put(user, delay);
        } else {
            if (delays.get(user) <= System.currentTimeMillis()) {
                randomCoinXp(channel, user, profile);
                long delay = System.currentTimeMillis() + (60 * 1000);
                delays.remove(user);
                delays.put(user, delay);

            }
        }
        if (message.getMentionedMembers().size() >= mentionThreshold) {
            if (SourceRole.ignoresModeration(message.getMember())) return;
            final SourceMute mute = new SourceMute(
                    event.getJDA().getSelfUser().getId(),
                    message.getAuthor().getId(),
                    "10 minutes",
                    String.format("Mention Spam Threshold Reached (%d Members)", mentionThreshold)
            );
            mute.sendIncidentEmbed();
            mute.execute();
        }
    }

    private boolean containsBlockedFiles(Message message) {

        if (SourceRole.ignoresModeration(message.getMember())) {
            return false;
        }

        List<Message.Attachment> attachmentList = message.getAttachments();

        List<String> blockedExtensions = Arrays.asList(blockedFileExtensions);
        for (Message.Attachment attachment : attachmentList) {
            String fileExtension = attachment.getFileExtension();
            if (blockedExtensions.contains(fileExtension)) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("Please use a bin rather than posting a file containing your code!");

                MessageChannel channel = message.getChannel();

                User user = message.getAuthor();
                channel.sendMessage(alert.build(user)).queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));

                message.delete().queue();
                return true;
            }
        }
        return false;
    }

    /**
     * The logic behind the random coins and xp
     *
     * @param channel The channel the message was sent in
     * @param user    The user receiving the coins or xp
     * @param profile The user's profile
     */
    private void randomCoinXp(TextChannel channel, User user, SourceProfile profile) {
        double xpbooster = profile.getXpBooster();
        double coinbooster = profile.getCoinBooster();

        long randomxp = ThreadLocalRandom.current().nextLong(15, 25 * (long) xpbooster);
        long randomcoins = (long) Math.ceil(ThreadLocalRandom.current().nextLong(3, 8 + 1) * coinbooster);
        int coinchance = ThreadLocalRandom.current().nextInt(1, (int) Math.round(15 / coinbooster));

        if (coinchance == 1) {
            long coinsToAdd = (long) (randomcoins * coinbooster);
            profile.addCoins(coinsToAdd);

            Category information = SourceChannel.INFORMATION_CATEGORY.resolve(channel.getJDA());
            if (channel.getParent() != null) {
                Category category = channel.getParent();
                if (category == information) return;
            }
            for (SourceChannel found : SourceChannel.noCoinNotifications) {
                if (found.resolve(channel.getJDA()) == channel) return;
            }

            if (profile.getCoinMessageToggle()) {
                ColoredAlert alert = new ColoredAlert(SourceColor.ORANGE);
                alert.setDescription("**" + user.getName() + "**, " + coinsToAdd + " coins have been added to your balance!");
                channel.sendMessage(alert.build(user, "+" + coinsToAdd + " Coins!")).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            }


        }

        profile.addXp(randomxp);
    }
}
