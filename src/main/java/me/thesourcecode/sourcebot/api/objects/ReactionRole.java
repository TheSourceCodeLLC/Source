package me.thesourcecode.sourcebot.api.objects;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ReactionRole {

    private static final Source source = Source.getInstance();
    private static final MongoCollection<Document> settingsCollection = source.getDatabaseManager().getCollection("Settings");

    private static final CACHE CACHE = new CACHE();

    private String shortcode;
    private String roleId;

    private String restrictedRoleId = null;
    private String oldShortcode = null;

    public ReactionRole(String shortcode, String roleId) {
        this.shortcode = shortcode.toLowerCase();
        this.roleId = roleId;
    }

    // Getters

    @Nullable
    public static ReactionRole getRRoleByShortCode(@NotNull String shortcode) {
        return CACHE.getRRoleBySC(shortcode);
    }

    @NotNull
    public static LinkedList<ReactionRole> getAllReactionRoles() {
        return new LinkedList<>(CACHE.reactionRoleCache.values());
    }

    public static void cacheAllReactionRoles() {
        Document query = new Document("name", "reactionRoles");
        Document reactionRolesDocument = settingsCollection.find(query).first();

        if (reactionRolesDocument == null) return;

        Document reactionRolesList = (Document) reactionRolesDocument.get("reactionRoles");
        Document restrictedRoleList = (Document) reactionRolesDocument.get("restrictedRoles");
        reactionRolesList.forEach((shortcode, object) -> {
            String roleId = (String) object;
            ReactionRole rrole = new ReactionRole(shortcode, roleId);

            if (restrictedRoleList != null && restrictedRoleList.containsKey(shortcode)) {
                String restrictedId = restrictedRoleList.getString(shortcode);
                rrole.setRestrictedRoleId(restrictedId);
            }

            CACHE.insertReactionRole(rrole);
        });

    }

    // Setters

    @NotNull
    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(@NotNull String shortcode) {
        oldShortcode = shortcode;
        this.shortcode = shortcode;
        updateMongoAndCache();
    }

    @NotNull
    public String getRoleId() {
        return roleId;
    }
    // Mongo methods

    public void setRoleId(@NotNull String roleId) {
        this.roleId = roleId;
        updateMongoAndCache();
    }

    @Nullable
    public String getRestrictedRoleId() {
        return restrictedRoleId;
    }

    public void setRestrictedRoleId(@Nullable String restrictedRoleId) {
        this.restrictedRoleId = restrictedRoleId;
        updateMongoAndCache();
    }

    public void createReactionRole() {
        Document query = new Document("name", "reactionRoles");
        Document reactionRolesDocument = retrieveReactionRolesDocument();

        Document reactionRolesList = (Document) reactionRolesDocument.get("reactionRoles");
        if (reactionRolesList.containsKey(shortcode)) {
            throw new IllegalArgumentException("There is already a reaction role for this shortcode!");
        }

        reactionRolesList.put(shortcode, roleId);
        reactionRolesDocument.put("reactionRoles", reactionRolesList);

        settingsCollection.updateOne(query, new Document("$set", reactionRolesDocument));
        CACHE.insertReactionRole(this);
    }


    // Static Methods

    public void deleteReactionRole() {
        Document query = new Document("name", "reactionRoles");
        Document reactionRolesDocument = retrieveReactionRolesDocument();

        Document reactionRolesList = (Document) reactionRolesDocument.get("reactionRoles");
        Document restrictedRoleList = (Document) reactionRolesDocument.get("restrictedRoles");
        restrictedRoleList = restrictedRoleList == null ? new Document() : restrictedRoleList;

        if (!reactionRolesList.containsKey(shortcode)) {
            throw new IllegalArgumentException("There is no reaction role for this shortcode!");
        }

        reactionRolesList.remove(shortcode);
        reactionRolesDocument.put("reactionRoles", reactionRolesList);

        restrictedRoleList.remove(shortcode);
        reactionRolesDocument.put("restrictedRoles", restrictedRoleList);

        settingsCollection.updateOne(query, new Document("$set", reactionRolesDocument));
        CACHE.removeReactionRole(this);
    }

    private void updateMongoAndCache() {
        Document query = new Document("name", "reactionRoles");
        Document reactionRolesDocument = retrieveReactionRolesDocument();

        String newShortCode = oldShortcode == null ? shortcode : oldShortcode;

        Document reactionRolesList = (Document) reactionRolesDocument.get("reactionRoles");
        if (!reactionRolesList.containsKey(newShortCode)) {
            throw new IllegalArgumentException("There is no reaction role for this shortcode!");
        }

        reactionRolesList.remove(newShortCode);
        reactionRolesList.put(shortcode, roleId);
        reactionRolesDocument.put("reactionRoles", reactionRolesList);


        Document restrictedRolesList = (Document) reactionRolesDocument.get("restrictedRoles");
        restrictedRolesList = restrictedRolesList == null ? new Document() : restrictedRolesList;

        restrictedRolesList.remove(newShortCode);
        restrictedRolesList.put(newShortCode, restrictedRoleId);
        reactionRolesDocument.put("restrictedRoles", restrictedRolesList);


        settingsCollection.updateOne(query, new Document("$set", reactionRolesDocument));
        CACHE.updateReactionRole(newShortCode, this);
        oldShortcode = null;
    }

    private Document retrieveReactionRolesDocument() {
        Document query = new Document("name", "reactionRoles");
        Document reactionRolesDocument = settingsCollection.find(query).first();

        if (reactionRolesDocument == null) {
            reactionRolesDocument = new Document("name", "reactionRoles")
                    .append("reactionRoles", new Document())
                    .append("restrictedRoles", new Document());
            settingsCollection.insertOne(reactionRolesDocument);
        }
        return reactionRolesDocument;
    }

    private static final class CACHE {
        private final Map<String, ReactionRole> reactionRoleCache = new LinkedHashMap<>();

        public void insertReactionRole(@NotNull ReactionRole reactionRole) {
            reactionRoleCache.put(reactionRole.shortcode, reactionRole);
        }

        public void updateReactionRole(@NotNull String oldShortCode, @NotNull ReactionRole reactionRole) {
            reactionRoleCache.remove(oldShortCode);
            reactionRoleCache.put(reactionRole.shortcode, reactionRole);
        }

        public void removeReactionRole(@NotNull ReactionRole reactionRole) {
            reactionRoleCache.remove(reactionRole.shortcode);
        }

        @Nullable
        public ReactionRole getRRoleBySC(@NotNull String shortcode) {
            return reactionRoleCache.get(shortcode);
        }
    }


}
