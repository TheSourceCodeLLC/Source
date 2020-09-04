package me.thesourcecode.sourcebot.api.entity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.jetbrains.annotations.NotNull;

public enum SourceChannel {
    INFORMATION_CATEGORY(ChannelType.CATEGORY, 356191412653654036L, 639851428566794278L),
    RULES(ChannelType.TEXT, 315275716595941377L, 639851431528235027L),
    AGREE(ChannelType.TEXT, 491387395825074199L, 639851433985835008L),
    CONNECTIONS(ChannelType.TEXT, 392144123165147136L, 639851437228032000L),
    ROLES(ChannelType.TEXT, 691357428427784202L, 691329827063464040L),
    YOUTUBE(ChannelType.TEXT, 365608965645533184L, 639851442781552680L),
    ANNOUNCEMENTS(ChannelType.TEXT, 274539777431437312L, 639851445188952064L),

    GENERAL_CATEGORY(ChannelType.CATEGORY, 356187890260115456L, 639851448078696449L),
    GENERAL_TEXT(ChannelType.TEXT, 265499275088232448L, 639851454370414602L),
    VOICE_CHAT(ChannelType.TEXT, 523563500119916544L, 639851467804770355L),
    DEVELOPMENT(ChannelType.TEXT, 504737411327328266L, 639851458870640641L),
    DONATIONS(ChannelType.TEXT, 497105166420672522L, 639851476151173161L),
    MEMES(ChannelType.TEXT, 320287823745777694L, 639851464847654951L),
    MUSIC(ChannelType.TEXT, 411651185800380416L, 639851471286042644L),
    DEVELOPERS(ChannelType.TEXT, 265962396542173194L, 639851479657611322L),
    DEV_LEADERS(ChannelType.TEXT, 497150374185009172L, 639851481968934942L),

    HELP_CATEGORY(ChannelType.CATEGORY, 362300193309458432L, 639851483889664000L),
    SPIGOT(ChannelType.TEXT, 265499790324793344L, 639851490105622559L),
    JAVA(ChannelType.TEXT, 402150208512983043L, 639851495558479873L),
    DISCORD(ChannelType.TEXT, 402150235071184918L, 639851501568786433L),
    DISCORD2(ChannelType.TEXT, 688780982081290277L, 0L),
    JAVASCRIPT(ChannelType.TEXT, 432965481486483457L, 639851506685706241L),
    OTHER_HELP(ChannelType.TEXT, 266677760666238976L, 639851518572363810L),

    COMMUNITY_CATEGORY(ChannelType.CATEGORY, 385523340636454935L, 639851523588882433L),
    COLLABORATE(ChannelType.TEXT, 329410015381422090L, 639851532132679680L),
    FREE_GAMES(ChannelType.TEXT, 524642336244760577L, 639851529049866240L),
    PROJECTS(ChannelType.TEXT, 265962692068638731L, 639851537396400157L),
    SUGGESTIONS(ChannelType.TEXT, 496060906971725844L, 639851543079813121L),

    BOT_CATEGORY(ChannelType.CATEGORY, 409137082234306570L, 639851555037904896L),
    INCIDENTS(ChannelType.TEXT, 414496524148539392L, 639851557160222721L),
    COMMANDS(ChannelType.TEXT, 411395024052551682L, 639851559169294337L),
    CHANGELOG(ChannelType.TEXT, 409581785723437056L, 639851563271192602L),

    STAFF_CATEGORY(ChannelType.CATEGORY, 356182358002761729L, 639851567134015510L),
    REPORTS(ChannelType.TEXT, 397075376666443776L, 639851569545871360L),
    MESSAGE_LOG(ChannelType.TEXT, 437380305943396362L, 639851571668189194L),
    MODS(ChannelType.TEXT, 592368968401158144L, 639851577024184331L),
    ADMINS(ChannelType.TEXT, 336596791292067851L, 639851580224700427L),
    APPLICATIONS(ChannelType.TEXT, 701116229938708502L, 0L),

    VOICE_CATEGORY(ChannelType.CATEGORY, 356185709364772874L, 639851584532119583L),
    GENERAL_VOICE(ChannelType.VOICE, 265499275088232449L, 639851592270610452L),
    SPECIAL_VOICE(ChannelType.VOICE, 301175103906119691L, 639851601498079251L),
    DEVELOPER_VOICE(ChannelType.VOICE, 421012145010901003L, 639851597710622721L),
    AFK_VOICE(ChannelType.VOICE, 265961783502569472L, 639851605914681376L),
    STAFF_VOICE(ChannelType.VOICE, 392451848855879680L, 639851612168388638L);

    public static final SourceChannel[] noCoinNotifications = {
            SUGGESTIONS, PROJECTS, FREE_GAMES
    };

    public static final SourceChannel[] ignoreGhost = {
            SUGGESTIONS, AGREE, REPORTS, FREE_GAMES, ANNOUNCEMENTS, YOUTUBE
    };

    public static final SourceChannel[] devHelpAndCommands = {
            COMMANDS, SPIGOT, JAVA, DISCORD, DISCORD2, JAVASCRIPT, OTHER_HELP, DEVELOPERS, DEV_LEADERS
    };

    private final ChannelType channelType;
    private final long mainID, betaID;

    SourceChannel(ChannelType channelType, long mainID, long betaID) {
        this.channelType = channelType;
        this.mainID = mainID;
        this.betaID = betaID;
    }

    /***
     *
     * @param jda The JDA to resolve with
     * @return The casted channel if it exists, otherwise null
     */

    @NotNull
    public <T extends GuildChannel> T resolve(JDA jda) {
        Guild main = SourceGuild.MAIN.resolve(jda);
        Guild beta = SourceGuild.BETA.resolve(jda);
        boolean isBeta = main == null;
        Guild guild = (isBeta) ? beta : main;
        long channelID = (isBeta) ? betaID : mainID;

        GuildChannel channel = null;
        switch (channelType) {
            case CATEGORY:
                channel = guild.getCategoryById(channelID);
                break;
            case TEXT:
                channel = guild.getTextChannelById(channelID);
                break;
            case VOICE:
                channel = guild.getVoiceChannelById(channelID);
                break;
        }
        return (T) channel;
    }

    /***
     *
     * @param channel The channel to check equality of
     * @param <T> Generic bound for JDA channel
     * @return Whether or not this is the specified channel
     */
    public <T extends GuildChannel> boolean isChannel(T channel) {
        long channelID = channel.getIdLong();
        return mainID == channelID || betaID == channelID;
    }
}
