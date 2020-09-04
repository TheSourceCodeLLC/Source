package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CapsListener extends AbstractListener<GuildMessageReceivedEvent> {

    private static final Pattern codeBlocks = Pattern.compile("(``)?`(.*?)`(``)?");

    public CapsListener() {
        super(GuildMessageReceivedEvent.class);
    }

    public static boolean checkCaps(String givenContent, Message message) {
        boolean runLogic = message != null;

        Member member = null;
        String content = null;
        if (runLogic) {
            member = message.getMember();
            if (member == null) return false;
            if (member.getUser().isBot()) {
                return false;
            }

            Category devHelp = SourceChannel.HELP_CATEGORY.resolve(message.getJDA());


            if (message.getTextChannel().getParent() == devHelp) {
                content = message.getContentRaw();

                Matcher matcher = codeBlocks.matcher(content);

                if (matcher.find()) {
                    return false;
                }

            }
        }


        content = content == null ? (message == null ? givenContent : message.getContentRaw()) : content;

        if (content.startsWith("```")) {
            //Ignore codeblocks
            return false;
        }
        content = content.replaceAll("<@.*?>", "");
        content = content.replaceAll("[^A-Za-z0-9]", "");
        boolean isCaps = false;
        char[] letters = content.replace(" ", "").toCharArray();
        int length = letters.length;

        if (length < 15) {
            return false;
        }
        int threshold = (int) (letters.length * 0.80) + 1;
        int caps = 0;
        for (char letter : letters) {
            if (Pattern.matches("[A-Z]", letter + "")) {
                if (isCaps) {
                    break;
                }
                caps++;
                isCaps = caps >= threshold;
            }
        }
        if (!isCaps) {
            return false;
        }
        if (!runLogic) return true;
        //The message is a caps violation
        if (SourceRole.ignoresModeration(member)) {
            return false;
        }

        message.delete().queue();
        String mention = member.getAsMention();
        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Caps Violation!").setDescription("That was an excessive amount of caps, " + mention + "!");
        message.getChannel().sendMessage(alert.build(member.getUser())).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
        return true;
    }

    @Override
    public void accept(GuildMessageReceivedEvent event) {
        checkCaps(null, event.getMessage());
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new CapsListener.CapsEditListener().listen(listener);
    }

    private final class CapsEditListener extends AbstractListener<GuildMessageUpdateEvent> {

        CapsEditListener() {
            super(GuildMessageUpdateEvent.class);
        }

        @Override
        public void accept(GuildMessageUpdateEvent event) {
            checkCaps(null, event.getMessage());
        }
    }

}
