package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourceMute extends SourceIncident {

    private static final Role mutedRole = SourceRole.MUTED.resolve(jda);

    private String duration;

    public SourceMute(String staffId, String targetId, String duration, String reason) {
        super(staffId, targetId, IncidentType.TEMPMUTE, reason);

        this.duration = duration;

        Document incidentDocument = getIncidentDocument();
        incidentDocument.append("DURATION", duration);

        setIncidentDocument(incidentDocument);
        insertDocument();
        createPunishmentHandlerDocument();
    }

    @NotNull
    public String getDuration() {
        return duration;
    }

    private void createPunishmentHandlerDocument() {
        User target = jda.getUserById(getTargetId());

        String[] durationArgs = getDuration().split("\\s+");

        long durationTime = Long.parseLong(durationArgs[0]);
        String durationType = durationArgs[1];

        long endTime = Utility.getUnpunishTime(durationType, durationTime);

        List<String> roleIds = new ArrayList<>();
        if (target != null) {
            Member member = guild.getMember(target);

            if (member != null) {
                roleIds = member.getRoles().stream()
                        .map(Role::getId)
                        .collect(Collectors.toList());

                roleIds.removeIf(roleId -> roleId.equals(mutedRole.getId()));
            }
        }
        Document mongoPunish = new Document("ID", getTargetId())
                .append("TYPE", getIncidentType().toString())
                .append("END", endTime)
                .append("ROLES", roleIds);


        dbManager.getCollection("PunishmentHandler").insertOne(mongoPunish);
    }

    @Override
    public boolean execute() {
        try {
            Member targetMember = guild.retrieveMemberById(getTargetId()).complete();

            List<Role> modifiedRoles = targetMember.getRoles().stream()
                .filter(Role::isManaged)
                .collect(Collectors.toList());
            modifiedRoles.add(mutedRole);

            // Adds the mute role and removes all other roles
            guild.modifyMemberRoles(targetMember, modifiedRoles).complete();
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());
        User targetUser = jda.getUserById(getTargetId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetUser == null ? "(" + getTargetId() + ")" : targetUser.getAsTag() + " (" + getTargetId() + ")";

        WarningAlert incidentAlert = new WarningAlert();
        incidentAlert.setDescription("**Tempmuted By:** " + staffInfo + "\n" +
                "**Tempmuted User:** " + targetInfo + "\n" +
                "**Duration:** " + getDuration() + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Tempmuted | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            try {
                targetUser.openPrivateChannel().complete().sendMessage(incidentEmbed).complete();
            } catch (Exception ignored) {
            }
        }
    }
}