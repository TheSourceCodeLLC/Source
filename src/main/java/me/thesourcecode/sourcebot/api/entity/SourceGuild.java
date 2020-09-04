package me.thesourcecode.sourcebot.api.entity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public enum SourceGuild {
    MAIN(265499275088232448L),
    BETA(487814013363814400L);

    private final long guildId;

    SourceGuild(long guildId) {
        this.guildId = guildId;
    }

    /***
     *
     * @param other The Guild to check Source validity for
     * @return True if this is a Source-managed Guild, false otherwise
     */
    public static boolean isSupported(Guild other) {
        long guildId = other.getIdLong();
        return guildId == MAIN.getGuildId() || guildId == BETA.getGuildId();
    }

    /***
     *
     * @return The Guild ID for this SourceGuild
     */
    public long getGuildId() {
        return guildId;
    }

    /***
     *
     * @param jda The JDA to try resolving this Guild with
     * @return The resolved Guild
     */
    public Guild resolve(JDA jda) {
        return jda.getGuildById(guildId);
    }
}
