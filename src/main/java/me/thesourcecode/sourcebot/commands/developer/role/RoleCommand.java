package me.thesourcecode.sourcebot.commands.developer.role;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import java.util.Arrays;
import java.util.HashMap;

public class RoleCommand extends Command {
    private static final CommandInfo INFO = new CommandInfo(
            "role",
            "Manage user profiles.",
            "<add|remove|list>",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEV_LEAD, SourceRole.ADMIN, SourceRole.OWNER)
            .withAliases("roles");

    protected static SourceRole[] LOG_ROLES = {
            SourceRole.DEV_LEAD, SourceRole.DEV
    };

    public RoleCommand() {
        registerSubcommand(new RoleListCommand());
        registerSubcommand(new RoleAddCommand());
        registerSubcommand(new RoleRemoveCommand());
    }

    /**
     * Gets all the roles that a user can add
     *
     * @param source  A source object
     * @param message A message object
     * @return A hashmap containing all of the roles a user can add
     */
    protected static HashMap<Integer, Role> getRoleList(Source source, Message message) {
        HashMap<Integer, Role> roleList = new HashMap<>();
        JDA jda = source.getJda();

        Guild guild = source.getGuild();
        Member member = Utility.getMemberByIdentifier(guild, message.getAuthor().getId());

        if (SourceRole.getRolesFor(member).contains(SourceRole.ADMIN)) {
            Role sourceRole = SourceRole.SOURCE.resolve(jda);
            guild.getRoles().forEach(role -> {
                if (!role.getId().equals(guild.getId())) {
                    if (role.getPosition() < sourceRole.getPosition()) {
                        roleList.put(roleList.size() + 1, role);
                    }
                }
            });

            return roleList;
        } else {
            Arrays.asList(SourceRole.DEVELOPERS).forEach(role -> {
                roleList.put(roleList.size() + 1, role.resolve(jda));
            });
            return roleList;
        }
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        return null;
    }
}
