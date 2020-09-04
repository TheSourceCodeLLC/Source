package me.thesourcecode.sourcebot.api.manager;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PunishmentManager {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final MongoCollection punishments;
    private final MongoCollection userPunishments;
    private final Guild guild;
    private final Role muteRole;

    public PunishmentManager(Source source) {
        boolean beta = source.isBeta();
        JDA jda = source.getJda();
        DatabaseManager databaseManager = source.getDatabaseManager();
        this.punishments = databaseManager.getCollection("PunishmentHandler");
        this.userPunishments = databaseManager.getCollection("Punishments");
        this.guild = (beta ? SourceGuild.BETA : SourceGuild.MAIN).resolve(jda);
        this.muteRole = SourceRole.MUTED.resolve(jda);
        checkPunishments();
        checkPoints();
    }

    private void checkPoints() {
        executorService.scheduleAtFixedRate(() -> {
            for (Object o : userPunishments.find()) {
                Document document = (Document) o;
                ArrayList<String> decay = (ArrayList<String>) document.get("decay");
                for (String decayInfo : decay) {
                    String[] decayArgs = decayInfo.split(" ");
                    double decayPoints = Double.valueOf(decayArgs[0]);
                    long decayTime = Long.valueOf(decayArgs[1]);

                    double points = document.getDouble("points");
                    if (decayTime <= System.currentTimeMillis()) {
                        points = points - decayPoints;

                        BigDecimal bd = new BigDecimal(Double.toString(points));
                        bd = bd.setScale(1, RoundingMode.HALF_UP);
                        points = bd.doubleValue();
                        decay.remove(decayInfo);

                        Document query = new Document("id", document.getString("id"));
                        document.append("points", points);
                        userPunishments.updateOne(query, new Document("$set", document));

                    }
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private void checkPunishments() {
        BasicDBList clauses = new BasicDBList();
        clauses.add(new Document("TYPE", "TEMPBAN"));
        clauses.add(new Document("TYPE", "TEMPMUTE"));
        clauses.add(new Document("TYPE", "BLACKLIST"));
        Document search = new Document("$or", clauses);
        executorService.scheduleAtFixedRate(() -> {

            for (Object o : punishments.find(search)) {
                try {
                    Document document = (Document) o;

                    long end = document.getLong("END");
                    long now = System.currentTimeMillis();

                    if (now < end) {
                        continue;
                    }

                    String type = document.getString("TYPE");
                    String memberID = document.getString("ID");
                    Member member = null;
                    if (!type.equalsIgnoreCase("TEMPBAN")) member = guild.getMemberById(memberID);

                    switch (type.toUpperCase()) {
                        case "TEMPBAN":
                            try {
                                guild.unban(memberID).complete();
                            } catch (ErrorResponseException ignored) {
                                // This error is fired when there is no user found
                            }
                            //TODO: Log in Incident Log
                            punishments.deleteOne(document);
                            break;
                        case "TEMPMUTE":
                            if (member == null) {
                                punishments.deleteOne(document);
                                continue;
                            }

                            if (document.get("ROLES") != null) {
                                List<Role> roles = ((ArrayList<String>) document.get("ROLES")).stream()
                                        .map(guild::getRoleById).filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                                // Adds all of the user's previous roles, and removes the mute role
                                guild.modifyMemberRoles(member, roles).queue();

                            } else {
                                guild.removeRoleFromMember(member, muteRole).queue();
                            }


                            //TODO: Log in Incident Log
                            punishments.deleteOne(document);
                            break;
                        case "BLACKLIST":
                            if (member == null) {
                                punishments.deleteOne(document);
                                continue;
                            }

                            String category = document.getString("CATEGORY");
                            category = category == null ? "DEVELOPMENT" : category;
                            switch (category.toUpperCase()) {
                                case "DEVELOPMENT":
                                    guild.removeRoleFromMember(member, SourceRole.BLACKLIST.resolve(guild.getJDA())).queue();
                                    break;
                                default:
                                    break;
                            }
                            // TODO: Log in Incident Log
                            punishments.deleteOne(document);
                            break;
                        default:
                            break;
                    }
                } catch (Exception ex) {
                    //ex.printStackTrace();
                }
            }


        }, 0, 15, TimeUnit.SECONDS);
    }
}
