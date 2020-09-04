package me.thesourcecode.sourcebot.api.objects.incidents;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class SourceIncident {

    protected static final TextChannel incidentLog = SourceChannel.INCIDENTS.resolve(jda);
    private static final Source source = Source.getInstance();
    protected static final JDA jda = source.getJda();
    protected static final Guild guild = source.getGuild();
    protected static final DatabaseManager dbManager = source.getDatabaseManager();
    private static final MongoCollection<Document> incidentCollection = dbManager.getCollection("IncidentReports");
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
    private long caseId;
    private IncidentType incidentType;
    private String dateTime;

    private String staffId;
    private String targetId;

    private String reason;

    private long timeCreatedInMs;
    private Document incidentDocument;

    public SourceIncident(@NotNull String staffId, @NotNull String targetId,
                          @NotNull IncidentType incidentType, @NotNull String reason) {
        this.caseId = incidentCollection.countDocuments() + 1;
        this.staffId = staffId;
        this.targetId = targetId;
        this.incidentType = incidentType;
        this.reason = reason;
        this.timeCreatedInMs = System.currentTimeMillis();

        LocalDateTime localDate = LocalDateTime.now();
        this.dateTime = dateFormat.format(localDate);

        this.incidentDocument = createMongoIncident();
    }

    public long getCaseId() {
        return caseId;
    }

    @NotNull
    public String getStaffId() {
        return staffId;
    }

    // This can be either a channel or user
    @NotNull
    public String getTargetId() {
        return targetId;
    }

    @NotNull
    public IncidentType getIncidentType() {
        return incidentType;
    }

    @NotNull
    public String getTimeCreated() {
        return dateTime;
    }

    @NotNull
    public String getReason() {
        return reason;
    }

    public long getTimeCreatedInMs() {
        return timeCreatedInMs;
    }

    @NotNull
    public Document getIncidentDocument() {
        return incidentDocument;
    }

    protected void setIncidentDocument(@NotNull Document incidentDocument) {
        this.incidentDocument = incidentDocument;
    }

    protected Document createMongoIncident() {
        String punishVary = "(Punishments vary based on your punishment history)";
        String reason = getReason().replace(punishVary, "").trim();

        return new Document("CASE_ID", getCaseId())
                .append("TYPE", getIncidentType().toString())
                .append("DATE_TIME", getTimeCreated())
                .append("STAFF_ID", getStaffId())
                .append("TARGET_ID", getTargetId())
                .append("REASON", reason)
                .append("TIME_IN_MS", getTimeCreatedInMs());
    }

    protected void insertDocument() {
        incidentCollection.insertOne(getIncidentDocument());
    }

    public abstract void execute();

    public abstract void sendIncidentEmbed();

    enum IncidentType {
        BAN,
        TEMPBAN,
        TEMPMUTE,
        BLACKLIST,
        KICK,
        WARN,
        CLEAR,
        UNBAN
    }

}
