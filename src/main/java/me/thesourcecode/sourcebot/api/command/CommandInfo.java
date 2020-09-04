package me.thesourcecode.sourcebot.api.command;

import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;

public class CommandInfo {

    private final Category category;

    private final CommandMap subcommands = new CommandMap();
    private final String label, description;
    private final String arguments;
    private boolean guildOnly = false;
    private boolean allowControllers = false;
    private boolean cooldown = false;
    private String[] aliases = new String[0];
    private SourceRole[] controlRoles = new SourceRole[0];
    private SourceChannel[] usageChannels = new SourceChannel[0];

    /***
     *
     * @param label The label for this command
     * @param description The description for this command
     * @param arguments The arguments for this command, if any
     * @param category That category of the command
     */
    public CommandInfo(String label, String description, String arguments, Category category) {
        this.label = label;
        this.description = description;
        this.arguments = arguments;
        this.category = category;
    }

    public CommandInfo(String label, String description) {
        this(label, description, "", Category.GENERAL);
    }

    public CommandInfo(String label, String description, Category category) {
        this(label, description, "", category);
    }

    /**
     * Sets this command to be accepted by {@link me.thesourcecode.sourcebot.api.entity.SourceController}s, regardless of Role.
     *
     * @return This CommandInfo instance for chaining
     */
    public CommandInfo allowControllers() {
        this.allowControllers = true;
        return this;
    }

    /***
     * Set this command to be usable for Guilds only
     * @return This CommandInfo instance for chaining
     */
    public CommandInfo asGuildOnly() {
        this.guildOnly = true;
        return this;
    }

    /***
     *
     * @param aliases The aliases to register for this command
     * @return This CommandInfo instance for chaining
     */
    public CommandInfo withAliases(String... aliases) {
        this.aliases = aliases;
        return this;
    }

    /***
     *
     * @param controlRoles The roles allowed to use this command, otherwise all roles are allowed
     * @return This CommandInfo instance for chaining
     */
    public CommandInfo withControlRoles(SourceRole... controlRoles) {
        this.controlRoles = controlRoles;
        return this;
    }

    /***
     *
     * @param usageChannels The channels available for executing this command, otherwise all channels are allowed
     * @return This CommandInfo instance for chaining
     */
    public CommandInfo withUsageChannels(SourceChannel... usageChannels) {
        this.usageChannels = usageChannels;
        return this;
    }

    /***
     *
     * @return The category for this command
     */
    public Category getCategory() {
        return category;
    }

    public CommandInfo hasCooldown() {
        this.cooldown = true;
        return this;
    }

    /***
     *
     * @return The label of this command
     */
    public final String getLabel() {
        return label;
    }

    /***
     *
     * @return The description for this command
     */
    public final String getDescription() {
        return description;
    }

    /***
     *
     * @return The arguments for this command, if any
     */
    public final String getArguments() {
        return arguments;
    }

    /***
     *
     * @return Whether or not this command is usable outside of a Guild
     */
    public final boolean isGuildOnly() {
        return guildOnly;
    }

    /***
     *
     * @return The aliases for this command, if any
     */
    public final String[] getAliases() {
        return aliases;
    }

    /***
     *
     * @return The explicit roles that can execute this command, if any, otherwise all roles
     */
    public final SourceRole[] getUsableRoles() {
        return controlRoles;
    }

    /***
     *
     * @return The explicit channels this command can be run in, if any
     */
    public final SourceChannel[] getUsageChannels() {
        return usageChannels;
    }

    public CommandMap getSubcommands() {
        return subcommands;
    }

    public boolean allowsControllers() {
        return allowControllers;
    }

    public boolean getCooldown() {
        return cooldown;
    }

    @Override
    public final int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CommandInfo) && ((CommandInfo) obj).getLabel().equals(label);
    }

    public enum Category {
        ADMIN,
        MODERATOR,
        DEVELOPER,
        DEVELOPMENT,
        ECONOMY,
        GENERAL,
        UNLISTED
    }


}
