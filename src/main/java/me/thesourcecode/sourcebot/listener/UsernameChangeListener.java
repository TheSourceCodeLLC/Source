package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;

import java.util.HashMap;
import java.util.Map;

public final class UsernameChangeListener extends AbstractListener<UserUpdateNameEvent> {
    private static final Map<String, Member> nameMap = new HashMap<>();
    private static Guild guild;

    public UsernameChangeListener(Source source) {
        super(UserUpdateNameEvent.class);
        guild = (source.isBeta() ? SourceGuild.BETA : SourceGuild.MAIN).resolve(source.getJda());
        guild.getMembers().forEach(m -> {
            User u = m.getUser();
            nameMap.put(String.format("%#s", u), m);
        });
    }

    public static Member getMemberByName(String username) {
        return nameMap.computeIfAbsent(username, $ -> {
            for (Member m : guild.getMembers()) {
                User u = m.getUser();
                String n = String.format("%#s", u);
                if (username.equals(n)) {
                    return m;
                }
            }
            return null;
        });
    }

    @Override
    public void accept(UserUpdateNameEvent event) {
        User user = event.getUser();
        Member member = guild.getMember(user);
        if (member == null) {
            return;
        }
        String name = event.getNewName();
        nameMap.put(name, member);

        SourceProfile userProfile = new SourceProfile(user);
        userProfile.setName(name);
    }
}
