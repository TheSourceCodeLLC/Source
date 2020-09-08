package me.thesourcecode.sourcebot.api;

import me.theforbiddenai.trellowrapperkotlin.TrelloApi;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.manager.PunishmentManager;
import me.thesourcecode.sourcebot.api.objects.ReactionRole;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import me.thesourcecode.sourcebot.api.objects.SourceSuggestion;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import me.thesourcecode.sourcebot.api.utility.Listener;
import me.thesourcecode.sourcebot.commands.administrator.*;
import me.thesourcecode.sourcebot.commands.administrator.giveaway.GiveawayCommand;
import me.thesourcecode.sourcebot.commands.administrator.reactionroles.ReactionRoleCommand;
import me.thesourcecode.sourcebot.commands.developer.UnblacklistCommand;
import me.thesourcecode.sourcebot.commands.developer.blacklist.BlacklistDevelopmentCommand;
import me.thesourcecode.sourcebot.commands.developer.role.RoleCommand;
import me.thesourcecode.sourcebot.commands.developer.tags.TagCommand;
import me.thesourcecode.sourcebot.commands.misc.*;
import me.thesourcecode.sourcebot.commands.misc.coinflip.CoinflipCommand;
import me.thesourcecode.sourcebot.commands.misc.doc.*;
import me.thesourcecode.sourcebot.commands.misc.profile.ProfileCommand;
import me.thesourcecode.sourcebot.commands.misc.report.ReportCommand;
import me.thesourcecode.sourcebot.commands.misc.suggestion.SuggestionCommand;
import me.thesourcecode.sourcebot.commands.moderation.*;
import me.thesourcecode.sourcebot.commands.moderation.punish.PunishCommand;
import me.thesourcecode.sourcebot.listener.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public class Source {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private static Source INSTANCE;
    private final JSONObject config;
    private final boolean beta;
    private JDA jda;
    private Listener listener;
    private CommandHandler commandHandler;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private TrelloApi trelloApi;
    private Guild guild;

    public Source(JSONObject config) throws LoginException, InterruptedException {
        this.config = config;
        this.beta = config.getBoolean("beta");
        String token = beta ? config.getString("token") : config.getString("mtoken");
        this.jda = login(token);

        jda.getPresence().setActivity(Activity.watching((beta) ? "TSC Beta" : "TSC"));


    }

    public static Source getInstance() {
        return INSTANCE;
    }

    private JDA login(String token) throws LoginException, InterruptedException {
        return new JDABuilder(token).build().awaitReady();
    }

    public void enable() {
        INSTANCE = this;

        JSONObject trello = config.getJSONObject("trelloApi");
        trelloApi = new TrelloApi(trello.getString("apikey"), trello.getString("token"));

        listener = new Listener(jda);
        commandHandler = new CommandHandler(this);
        databaseManager = new DatabaseManager(this, config.getJSONObject("database"));
        punishmentManager = new PunishmentManager(this);

        guild = beta ? SourceGuild.BETA.resolve(jda) : SourceGuild.MAIN.resolve(jda);

        SourceTag.cacheAllTags();
        SourceTag.updateTagsLoop();
        SourceProfile.maintainRankHierarchy();
        if (!beta) SourceSuggestion.removeImplementedSuggestions();
        ReactionRole.cacheAllReactionRoles();
    }

    public Guild getGuild() {
        return guild;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public JSONObject getConfig() {
        return config;
    }

    public boolean isBeta() {
        return beta;
    }

    public JDA getJda() {
        return jda;
    }

    public Listener getListener() {
        return listener;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TrelloApi getTrelloApi() {
        return trelloApi;
    }

    public void registerCommands() {
        Stream.of(
                new PingCommand(),
                new EightballCommand(),
                new HelpCommand(),
                new OnlineCommand(),
                new ProfileCommand(),
                new ServerInfoCommand(),
                new PayCommand(),
                new ReportCommand(),
                new ShopCommand(),
                new HistoryCommand(),
                new LeaderboardCommand(),
                new CoinflipCommand(),
                new DailyCommand(),
                new GambleCommand(),
                new CaseCommand(),
                new CoinleaderboardCommand(),
                new NPMCommand(),
                new SuggestionCommand(),
                new GFMSCommand(),
                new MinecraftRoleCommand(),

                new JDACommand(),
                new JavaCommand(),
                new JSCommand(),
                new DJSCommand(),
                new SpigotCommand(),
                new BungeeCordCommand(),
                new TagCommand(),

                new BlacklistDevelopmentCommand(),
                new UnblacklistCommand(),
                new UnmuteCommand(),
                new UnbanCommand(),
                new ClearCommand(),
                new RoleCommand(),
                new KickCommand(),
                new TempmuteCommand(),
                new TempbanCommand(),
                new SoftbanCommand(),
                new BanCommand(),
                new PunishCommand(),
                new BroadcastCommand(),
                new PollCommand(),
                new ReactCommand(),
                new GiveawayCommand(),
                new ReactionRoleCommand(),
                new RestartCommand(),
                new StopCommand(),
                new UpdateCommand(),

                new ChangelogCommand()
        ).forEach(commandHandler::registerCommand);
    }

    public void registerEvents() {
        Stream.of(
                new SuggestionVoteListener(this),
                new NicknameListener(this),
                new ConnectionListener(this),
                // new CodeBlockListener(),
                //new UserActivityListener(this),
                new RoleReactionListener(this),
                new DocReactionListener(),
                new CapsListener(),
                new InviteListener(),
                new UsernameChangeListener(this),
                new ShopListener(this),
                new NormalChatListener(),
                new VoiceListener(),
                new DocSelectionListener(),
                new ShowoffReactionListener(this),
                new TagListener(this),
                new ReportsListener(this)
        ).forEach(event -> event.listen(listener));
    }

}
