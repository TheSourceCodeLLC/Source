package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class ConnectionListener extends AbstractListener<Event> {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final TextChannel connections;

    public ConnectionListener(Source source) {
        super(Event.class);
        this.connections = SourceChannel.CONNECTIONS.resolve(source.getJda());
    }

    @Override
    public void listen(Listener listener) {
        new MemberJoinListener().listen(listener);
        new MemberLeaveListener().listen(listener);
    }

    @Override
    public void accept(Event event) {
    }

    private final class MemberJoinListener extends AbstractListener<GuildMemberJoinEvent> {
        private final String[] joinMessages = {
                "**%n** just joined the server - glhf!",
                "**%n** just joined. Everyone, look busy!",
                "**%n** just joined. Can I get a heal?",
                "**%n** joined your party.",
                "**%n** joined. You must construct additional pylons.",
                "Ermagherd. **%n** is here.",
                "Welcome, **%n**. Stay awhile and listen.",
                "Welcome, **%n**. We were expecting you ( ͡° ͜ʖ ͡°)",
                "Welcome, **%n**. We hope you brought pizza.",
                "Welcome **%n**. Leave your weapons by the door.",
                "A wild **%n** appeared.",
                "Swoooosh. **%n** just landed.",
                "Brace yourselves. **%n** just joined the server.",
                "**%n** just joined. Hide your bananas.",
                "**%n** just arrived. Seems OP - please nerf.",
                "**%n** just slid into the server.",
                "A **%n** has spawned in the server.",
                "Big **%n** showed up!",
                "Where’s **%n**? In the server!",
                "**%n** hopped into the server. Kangaroo!!",
                "**%n** just showed up. Hold my beer.",
        };

        MemberJoinListener() {
            super(GuildMemberJoinEvent.class);
        }

        @Override
        public void accept(GuildMemberJoinEvent event) {
            Member member = event.getMember();
            String message = joinMessages[random.nextInt(0, joinMessages.length)];
            String name = String.format("%#s", member.getUser());
            message = message.replace("%n", name);
            InfoAlert alert = new InfoAlert();
            alert.setDescription(message);
            connections.sendMessage(alert.build(null)).queue();
        }
    }

    private final class MemberLeaveListener extends AbstractListener<GuildMemberLeaveEvent> {
        private final String[] leaveMessages = {
                "**%n** just left the server - glhf!",
                "**%n** just left. Everyone, mess about while you can!",
                "**%n** just left. Can you come back?",
                "**%n** left your party.",
                "Ermagherd. **%n** has just left us here.",
                "Goodbye, **%n**. We were expecting you to stay( ͡° ͜ʖ ͡°)",
                "Goodbye, **%n**. We hope you brought pizza before you left.",
                "Goodbye **%n**. Take your weapons by the door.",
                "A wild **%n** disappeared.",
                "Swoooosh. **%n** just flew away.",
                "Brace yourselves. **%n** just abandoned the server.",
                "**%n** just left. Eat your bananas.",
                "**%n** just slid out of the server.",
                "A **%n** has just left the server",
                "Big **%n** disappeared!",
                "Where’s **%n**? did they leave the server?",
                "**%n** hopped out of the server. Kangaroo!!",
                "**%n** just left town. Hold my beer.",
                "Challenger down! - **%n** has disappeared!",
                "Ha! **%n** has left! I win!",
                "We were expecting you **%n** to stay",
                "It's dangerous to go alone, but **%n** left!",
                "Cheers, love! You made **%n** leave!",
                "**%n** has vanished, the prophecy has been ruined!",
                "**%n** has quit. Party's over.",
                "**%n**'s controller died!",
                "Hello. Is it **%n** you're looking for? He just left us!",
                "Roses are red, violets are blue, **%n** left, what did we do?"
        };

        MemberLeaveListener() {
            super(GuildMemberLeaveEvent.class);
        }

        @Override
        public void accept(GuildMemberLeaveEvent event) {
            User user = event.getUser();
            String message = leaveMessages[random.nextInt(0, leaveMessages.length)];
            String name = String.format("%#s", user);
            message = message.replace("%n", name);
            CriticalAlert alert = new CriticalAlert();
            alert.setDescription(message);
            connections.sendMessage(alert.build(null)).queue();
        }
    }
}
