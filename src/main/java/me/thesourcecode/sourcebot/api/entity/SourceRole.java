package me.thesourcecode.sourcebot.api.entity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SourceRole {
    //Overlord, Bot Developer
    OWNER(392139914944315392L, 639851409063542794L, true),
    ADMIN(332378762744430593L, 685957040694493240L, true),
    SOURCE(395299003794980864L, 685957096390656064L, true),
    MODERATOR(592361914282147857L, 639851411840172038L, true),
    MUTED(341673514774036500L, 639851413161115659L),
    BLACKLIST(537431780102242306L, 639851421373562881L),
    CONTENT_CREATOR(509394390146351117L, 639851413970878511L),

    //Dev leader
    DEV_LEAD(478577125226446848L, 639851414725722113L),

    //Code magician
    DEV(266677552712646666L, 639851415564451870L),

    //Language roles
    JAVA(420281847411507212L, 639851416147722261L),
    SPIGOT(504736090658177044L, 639851416541724686L),
    JDA(504736363263033344L, 639851417615728645L),
    JS(420281942655631362L, 639851417930170431L),
    DJS(504736044038488064L, 639851418848722975L),
    WEB(420282072406425600L, 639851419230273537L),
    DATABASE(420287502163640332L, 639851420413067325L),
    CPP(514166421710700616L, 639851419813412875L),

    //Sugar daddies
    DONOR(327527565680050178L, 639851422221074513L),
    NITRO_BOOSTER(581323238831947777L, 0L),

    // Holiday Roles
    SPOOKTOBER(762038305897578516L, 0L),

    //Level roles
    MVP(452215843632185355L, 639851424024494100L),
    VIP(452215658831151104L, 639851424674480139L),

    GMFS(524684493660094494L, 639851426998124544L),
    MINECRAFT(672061460414988288L, 0L),
    UNVERIFIED(491385493632581633L, 639851425425391638L),

    //@everyone
    EVERYONE(265499275088232448L, 487814013363814400L);

    public static final SourceRole[] STAFF = {
            OWNER, ADMIN
    };

    public static final SourceRole[] STAFF_MOD = {
            OWNER, ADMIN, MODERATOR
    };

    public static final SourceRole[] DEVELOPERS_STAFF = {
            OWNER, ADMIN, MODERATOR, DEV_LEAD, DEV
    };

    public static final SourceRole[] DEVELOPERS = {
            DEV_LEAD, DEV, JAVA, SPIGOT, JDA, JS, DJS, WEB, DATABASE, CPP
    };

    public static final SourceRole[] EXEMPT_FROM_ROLE_RESTRICTIONS = {
            NITRO_BOOSTER, DONOR, DEV, CONTENT_CREATOR
    };

    private final long mainID, betaID;
    private final boolean ignore;

    SourceRole(long mainID, long betaID, boolean ignore) {
        this.mainID = mainID;
        this.betaID = betaID;
        this.ignore = ignore;
    }

    SourceRole(long mainID, long betaID) {
        this(mainID, betaID, false);
    }

    /***
     *
     * @param member The member to get roles for
     * @return A list of SourceRoles this member has
     */
    public static List<SourceRole> getRolesFor(Member member) {
        SourceRole[] roles = SourceRole.values();
        List<SourceRole> rolesList = Arrays.asList(roles);

        JDA jda = member.getJDA();
        List<Role> memberRoles = member.getRoles();
        return rolesList.stream().filter(sr -> {
            Role resolved = sr.resolve(jda);
            if (resolved == null) {
                return false;
            }
            return memberRoles.contains(resolved);
        }).collect(Collectors.toList());
    }

    public static boolean ignoresModeration(Member member) {
        List<SourceRole> sourceRoles = SourceRole.getRolesFor(member);
        return sourceRoles.stream().map(SourceRole::ignoresModeration).findAny().orElse(false);
    }

    public boolean ignoresModeration() {
        return ignore;
    }

    /***
     *
     * @param jda The JDA to try resolving this role against
     * @return The role if successful, otherwise null
     */
    public Role resolve(JDA jda) {
        Guild main = SourceGuild.MAIN.resolve(jda);
        Guild beta = SourceGuild.BETA.resolve(jda);
        if (main != null) {
            return main.getRoleById(mainID);
        } else {
            return beta.getRoleById(betaID);
        }
    }

    /***
     *
     * @param role The role to check equality of
     * @return Whether or not this role is the specified role
     */
    public boolean isRole(Role role) {
        long roleID = role.getIdLong();
        return mainID == roleID || betaID == roleID;
    }

}
