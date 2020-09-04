package me.thesourcecode.sourcebot.api.command;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceController;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CommonAlerts;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Command {

    public final Message onCommand(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        if (user.isBot() || user.isFake()) return null;

        Command command = this;
        CommandInfo info = getInfo();
        final CommandMap subcommands = info.getSubcommands();

        Guild guild = source.getGuild();
        Member member = guild.getMember(user);
        if (member == null) throw new NullPointerException("Member can not be null.");

        String invalidSyntax = CommandHandler.getPrefix() + info.getLabel();

        if (args.length > 0) {
            String label = args[0].toLowerCase();
            Command child = subcommands.getCommand(label);
            if (child != null) {
                invalidSyntax += " " + child.getInfo().getLabel();
                StringBuilder invalidSyntaxBuilder = new StringBuilder(invalidSyntax);
                int copyOf = 1;

                CommandInfo childInfo = child.getInfo();
                if (args.length > 1) {
                    int retrieveArg = 1;
                    while (childInfo.getSubcommands().getCommand(args[retrieveArg].toLowerCase()) != null) {

                        String subCmdName = args[retrieveArg].toLowerCase();
                        child = childInfo.getSubcommands().getCommand(subCmdName);
                        childInfo = child.getInfo();

                        invalidSyntaxBuilder.append(" ").append(child.getInfo().getLabel());

                        retrieveArg++;
                        copyOf++;
                        if (retrieveArg >= args.length || retrieveArg > childInfo.getSubcommands().getCommandInfo().size())
                            break;
                    }

                }

                invalidSyntax = invalidSyntaxBuilder.toString().trim();
                info = childInfo;
                command = child;

                args = Arrays.copyOfRange(args, copyOf, args.length);
            }
        }
        invalidSyntax += " " + info.getArguments();
        JDA jda = message.getJDA();

        SourceRole[] usageRoles = info.getUsableRoles();
        SourceChannel[] usageChannels = info.getUsageChannels();

        if (message.getChannelType() == ChannelType.PRIVATE) {
            if (info.isGuildOnly()) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Guild-only Command!").setDescription("This command can only be used in TSC!");
                MessageEmbed embed = alert.build(message.getAuthor());
                return new MessageBuilder(embed).build();
            }

        }
        MessageChannel channel = message.getChannel();
        boolean ignoreUsageChannel;

        TextChannel verify = SourceChannel.AGREE.resolve(source.getJda());
        if (!SourceRole.ignoresModeration(member) && channel == verify) {

            CommandInfo verifyCmd = CommandHandler.getCommands().getCommand("verify").getInfo();

            List<String> aliases = new ArrayList<>(Arrays.asList(verifyCmd.getAliases()));
            aliases.add(verifyCmd.getLabel());

            boolean run = false;
            String[] checkArgs = message.getContentRaw().substring(1).split("\\s+");
            for (String check : aliases) {
                if (checkArgs[0].equalsIgnoreCase(check)) {
                    run = true;
                    break;
                }
            }
            if (!run) {
                return null;
            }


        }

        List<SourceRole> memberRoles = SourceRole.getRolesFor(member);
        if (usageRoles.length > 0) {
            if (Collections.disjoint(memberRoles, Arrays.asList(usageRoles))) {
                if (info.allowsControllers() && !SourceController.isValidController(message.getAuthor())) {
                    CommonAlerts embed = new CommonAlerts();
                    return new MessageBuilder(embed.noPermissions(member.getUser())).build();
                } else {
                    CommonAlerts embed = new CommonAlerts();
                    return new MessageBuilder(embed.noPermissions(member.getUser())).build();
                }
            }
        }
        ignoreUsageChannel = memberRoles.stream()
                .map(SourceRole::ignoresModeration)
                .findAny()
                .orElse(false);
        if (usageChannels.length > 0 && !ignoreUsageChannel) {
            List<TextChannel> channels = new ArrayList<>();
            Arrays.stream(usageChannels)
                    .map(sc -> sc.resolve(jda))
                    .filter(c -> c instanceof TextChannel || c instanceof Category)
                    .forEach(c -> {
                        if (c instanceof TextChannel) {
                            channels.add((TextChannel) c);
                        } else {
                            channels.addAll(((Category) c).getTextChannels());
                        }
                    });
            channels.sort((o1, o2) -> {
                String n1 = o1.getName();
                String n2 = o2.getName();
                return Collator.getInstance().compare(n1, n2);
            });
            if (channel.getType() != ChannelType.PRIVATE && !channels.contains(channel) && !memberRoles.stream().map(SourceRole::ignoresModeration).findFirst().orElse(false)) {
                List<String> channelMentions = channels.stream()
                        .filter(tc -> tc.canTalk(member))
                        .map(IMentionable::getAsMention)
                        .collect(Collectors.toList());
                String format = String.join(", ", channelMentions);
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Invalid Channel!").setDescription("This command may only be used in the following channel(s):\n" + format);
                MessageEmbed embed = alert.build(member.getUser());

                return new MessageBuilder(embed).build();
            }


        }

        if (info.getCooldown() && Utility.checkCooldown(message, info.getLabel(), false))
            return null;


        List<String> requiredCommandArgs = getRequiredCommandArgs(info);

        int requiredCommandArgsSize = requiredCommandArgs.size();
        if (requiredCommandArgsSize > 0) {
            if (requiredCommandArgsSize > args.length) {
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Incorrect Usage!").setDescription("Syntax: " + invalidSyntax);

                MessageEmbed embed = alert.build(message.getAuthor());
                return new MessageBuilder(embed).build();
            }
        }

        return command.execute(source, message, args);
    }

    /**
     * @param info The command's CommandInfo object
     * @return Any arg surround with <> aka required args in a List
     */
    private List<String> getRequiredCommandArgs(CommandInfo info) {
        String[] cmdArgs = info.getArguments().split("\\s+");
        return Arrays.stream(cmdArgs)
                .filter(cmdArg -> cmdArg.matches("<.*>"))
                .collect(Collectors.toList());
    }

    public abstract CommandInfo getInfo();

    /***
     *
     * @param message The message used in this command
     * @param args The args used in this command
     * @return The unsent message response, or null if none. {@see MessageBuilder}
     */
    public abstract Message execute(Source source, Message message, String[] args);

    /***
     *
     * @param command The subcommand to register
     * @param <T> The type of subcommand registered
     * @return The registered subcommand
     */
    public final <T extends Command> T registerSubcommand(T command) {
        return getInfo().getSubcommands().register(command);
    }
}
