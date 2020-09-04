package me.thesourcecode.sourcebot.api.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandMap {
    private final Set<CommandInfo> infoSet = new HashSet<>();
    private final Map<String, Command> commandMap = new HashMap<>();

    /***
     *
     * @param command The command to register
     * @param <T> Generic bound for Command
     * @return The registered command
     */
    public <T extends Command> T register(T command) {
        CommandInfo info = command.getInfo();
        infoSet.add(info);

        String label = info.getLabel();
        String[] aliases = info.getAliases();
        commandMap.put(label, command);
        for (String alias : aliases) {
            commandMap.put(alias, command);
        }
        return command;
    }

    /***
     *
     * @param label The command label to unregister
     * @return Whether or not the operation suceeded
     */
    public boolean unregister(String label) {
        CommandInfo info = infoSet.stream().filter(cInfo -> label.equals(cInfo.getLabel())).findFirst().orElse(null);
        if (info == null) {
            return false;
        }
        infoSet.remove(info);
        String[] aliases = info.getAliases();
        commandMap.remove(label);
        for (String alias : aliases) {
            commandMap.remove(alias);
        }
        return true;
    }

    /***
     *
     * @param identifier The identifier to search for, possibly an alias
     * @return The registered command, if any
     */
    public Command getCommand(String identifier) {
        return commandMap.get(identifier);
    }

    /***
     *
     * @return A set of the presently registered CommandInfo objects
     */
    public Set<CommandInfo> getCommandInfo() {
        return infoSet;
    }
}
