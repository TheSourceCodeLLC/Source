package me.thesourcecode.sourcebot.api.objects;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class SourceTag {

    public static final List<SourceTag> tagList = new ArrayList<>();
    private static final Cache CACHE = new Cache();
    private static final DatabaseManager dbManager = Source.getInstance().getDatabaseManager();
    private static final MongoCollection<Document> tagCollection = dbManager.getCollection("Tags");
    private String oldNameStorage = null;

    private Document mongoTag;

    private String name;
    private String description;
    private String categoryName;

    private List<String> aliases;
    private ArrayList<String> editIds;
    private int uses = 0;

    private String type;
    private String creatorId;
    private long timeCreatedInMs;

    public SourceTag(@NotNull String name) {
        name = name.toLowerCase();
        SourceTag tag = CACHE.getTag(name);

        if (tag == null) {
            Document mongoTag = queryTag(name);
            if (mongoTag == null) throw new NullPointerException("Tag not found!");

            insertTagFromDocument(mongoTag);

            CACHE.insertTag(name, this);
            getAliases().forEach(alias -> CACHE.insertTag(alias.toLowerCase(), this));
            if (!tagList.contains(this)) tagList.add(this);
        } else {
            insertTagFromObject(tag);
        }
    }

    /**
     * Used for creating tags
     *
     * @param creatorId   Id of user who is creating the tag
     * @param name        The name of the tag
     * @param description The description of the tag
     */
    public SourceTag(@NotNull String creatorId, @NotNull String name, @NotNull String description) {
        name = name.toLowerCase();
        Document mongoTag = createTag(creatorId, name, description);

        insertTagFromDocument(mongoTag);
        CACHE.insertTag(name, this);

        if (!tagList.contains(this)) tagList.add(this);
    }

    public static void cacheAllTags() {
        tagCollection.find().forEach((Block<? super Document>) document -> {
            String tagName = document.getString("name");

            SourceTag cacheTag = new SourceTag(tagName);
            if (!tagList.contains(cacheTag)) tagList.add(cacheTag);
        });
    }

    // This is to prevent db spam if someone spams a tag
    public static void updateTagsLoop() {
        Source.getInstance()
                .getExecutorService()
                .scheduleAtFixedRate(SourceTag::saveAllTags, 10, 10, TimeUnit.MINUTES);

    }

    // This is its own method so it can be called on stop/restart
    public static void saveAllTags() {
        CACHE.tagCache.values().forEach(sourceTag -> sourceTag.updateMongoTagAndCache(null));
    }

    public Document getMongoTag() {
        return mongoTag;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public ArrayList<String> getEditIds() {
        return editIds;
    }

    private void setEditIds(@NotNull ArrayList<String> editIds) {
        this.editIds = editIds;

        mongoTag.append("edit", editIds);
        updateMongoTagAndCache(null);
    }

    // Setters

    public String getType() {
        return type;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public long getTimeCreatedInMs() {
        return timeCreatedInMs;
    }

    public void setName(@NotNull String userId, @NotNull String name) {
        CACHE.removeTag(getName());
        CACHE.insertTag(name, this);

        oldNameStorage = getName();
        this.name = name;

        mongoTag.append("name", name);
        updateMongoTagAndCache(userId);
    }

    public void setDescription(@Nullable String userId, @NotNull String description) {
        this.description = description;

        mongoTag.append("description", description);
        updateMongoTagAndCache(userId);
    }

    public void setCategoryName(@Nullable String userId, @Nullable String categoryName) {
        this.categoryName = categoryName;

        mongoTag.append("category", categoryName);
        updateMongoTagAndCache(userId);
    }

    public void setAliases(@Nullable String userId, @NotNull List<String> aliases) {
        getAliases().forEach(CACHE::removeTag);

        aliases = aliases.stream().map(String::toLowerCase).collect(Collectors.toList());
        aliases.forEach(alias -> CACHE.insertTag(alias, this));

        this.aliases = aliases;

        mongoTag.append("aliases", aliases);
        updateMongoTagAndCache(userId);
    }

    // Extra methods

    public void setType(@Nullable String userId, boolean useEmbed) {
        this.type = useEmbed ? "embed" : "text";

        mongoTag.append("type", getType());
        updateMongoTagAndCache(userId);
    }

    public int getUses() {
        return uses;
    }

    private void setUses(int uses) {
        this.uses = uses;

        mongoTag.append("uses", uses);
        CACHE.updateTag(getName(), this);
        getAliases().forEach(alias -> CACHE.updateTag(alias, this));
    }

    public void refreshTag() {
        SourceTag tag = CACHE.getTag(getName());
        if (tag == null) return;

        insertTagFromObject(tag);
    }

    public void deleteTag() {
        tagCollection.deleteMany(getMongoTag());

        getAliases().forEach(CACHE::removeTag);
        CACHE.removeTag(getName());

        tagList.remove(this);
    }

    // Mongo Methods

    private void updateMongoTagAndCache(@Nullable String userId) {
        if (userId != null) {
            ArrayList<String> newEditIds = getEditIds();
            if (!newEditIds.contains(userId)) {
                newEditIds.add(userId);

                setEditIds(newEditIds);
            }
        }
        CACHE.updateTag(getName(), this);
        getAliases().forEach(alias -> CACHE.updateTag(alias, this));

        Document oldTag = queryTag(oldNameStorage != null ? oldNameStorage : getName());
        oldNameStorage = null;

        if (oldTag == null) return;
        if (oldTag == mongoTag) return;

        tagCollection.updateOne(oldTag, new Document("$set", mongoTag));
    }

    public void incrementUses() {
        setUses(getUses() + 1);
    }

    private void insertTagFromObject(SourceTag tag) {
        mongoTag = tag.getMongoTag();
        name = tag.getName();
        description = tag.getDescription();
        categoryName = tag.getCategoryName();

        aliases = tag.getAliases();
        editIds = tag.getEditIds();
        uses = tag.getUses();

        type = tag.getType();
        creatorId = tag.getCreatorId();
        timeCreatedInMs = tag.getTimeCreatedInMs();
    }

    // Static methods

    private void insertTagFromDocument(Document mongoTag) {
        this.mongoTag = mongoTag;
        name = mongoTag.getString("name");
        description = mongoTag.getString("description");
        categoryName = mongoTag.getString("category");

        aliases = mongoTag.containsKey("aliases") ? (ArrayList<String>) mongoTag.get("aliases") : new ArrayList<>();
        editIds = mongoTag.containsKey("edit") ? (ArrayList<String>) mongoTag.get("edit") : new ArrayList<>();
        uses = mongoTag.containsKey("uses") ? mongoTag.getInteger("uses") : 0;

        type = mongoTag.getString("type");
        creatorId = mongoTag.getString("userId");
        timeCreatedInMs = mongoTag.getLong("timeInMS");
    }

    @Nullable
    private Document queryTag(@NotNull String name) {
        Document mongoTag = tagCollection.find(new Document("name", name)).first();

        if (mongoTag == null) mongoTag = tagCollection.find(eq("aliases", name)).first();

        return mongoTag;
    }

    private Document createTag(@NotNull String creatorId, @NotNull String name, @NotNull String description) {
        Document newTag = new Document("name", name)
                .append("description", description)
                .append("type", "embed")
                .append("userId", creatorId)
                .append("timeInMS", System.currentTimeMillis())
                .append("category", "misc")
                .append("edit", new ArrayList<String>())
                .append("aliases", new ArrayList<>())
                .append("uses", 0);

        tagCollection.insertOne(newTag);
        return newTag;
    }

    private static final class Cache {

        private final Map<String, SourceTag> tagCache = new ConcurrentHashMap<>();

        @Nullable
        SourceTag getTag(@NotNull String mongoTag) {
            return tagCache.get(mongoTag.toLowerCase());
        }

        void updateTag(@NotNull String tagName, @NotNull SourceTag sourceTag) {
            tagCache.replace(tagName.toLowerCase(), sourceTag);
        }

        void insertTag(@NotNull String tagName, @NotNull SourceTag sourceTag) {
            tagCache.put(tagName.toLowerCase(), sourceTag);
        }

        void removeTag(@NotNull String tagName) {
            tagCache.remove(tagName.toLowerCase());
        }

    }

}
