package me.thesourcecode.sourcebot.api.objects;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SourceProfile {

    private static final Cache CACHE = new Cache();

    private static final Source source = Source.getInstance();
    private static final DatabaseManager dbManager = source.getDatabaseManager();
    private static final MongoCollection<Document> userData = dbManager.getCollection("UserData");

    private static final Guild GUILD = source.getGuild();
    private static final Role MVP = SourceRole.MVP.resolve(source.getJda());
    private static final Role VIP = SourceRole.VIP.resolve(source.getJda());

    private Document mongoProfile;

    private User user;
    private String id;
    private String name; // This is needed for the sourcebot website
    private String oldNickname;

    private long xp;
    private long coins;
    private long level; // So is this
    private int rank; // and this

    private double xpBooster;
    private double coinBooster;

    private int dailyStreak;

    private String github;
    private String bio;
    private boolean coinMessageToggle;

    private Document cooldowns;
    private ArrayList<String> badges;

    public SourceProfile(@NotNull User user) {
        this.user = user;
        id = user.getId();

        SourceProfile profile = CACHE.getProfileById(id);

        if (profile == null) {
            insertProfileFromDocument(queryProfile());
            CACHE.insertProfile(this);
        } else {
            insertProfileFromObject(profile);
        }
    }

    public SourceProfile(@NotNull Document mongoProfile) {
        insertProfileFromDocument(mongoProfile);

        String id = mongoProfile.getString("id");
        if (!CACHE.profileCache.containsKey(id)) {
            CACHE.insertProfile(this);
        }
    }

    // Getters

    public static void maintainRankHierarchy() {
        CACHE.xpRankCache.cacheUserRanks();
        CACHE.xpRankCache.sortLeaderboard();

        CACHE.coinRankCache.cacheUserRanks();
        CACHE.coinRankCache.sortLeaderboard();
    }

    public static LinkedHashMap<String, Integer> getRanksByPage(CacheType cacheType, int page) {
        switch (cacheType) {
            case XP:
                return CACHE.xpRankCache.getLbByPage(page);
            case COIN:
                return CACHE.coinRankCache.getLbByPage(page);
            default:
                return null;
        }

    }

    public static int getRankCacheSize(CacheType cacheType) {
        switch (cacheType) {
            case XP:
                return CACHE.xpRankCache.lbCache.size();
            case COIN:
                return CACHE.coinRankCache.lbCache.size();
            default:
                return -1;
        }

    }

    public Document getMongoProfile() {
        return mongoProfile;
    }

    public User getUser() {
        return user;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;

        mongoProfile.put("name", name);
        updateMongoProfileAndCache();
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;

        mongoProfile.put("xp", xp);
        updateMongoProfileAndCache();

        CACHE.xpRankCache.updateUser(this);

        balanceLevel();
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;

        mongoProfile.put("coins", coins);
        CACHE.coinRankCache.updateUser(this);
        updateMongoProfileAndCache();
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;

        mongoProfile.put("level", level);
        updateMongoProfileAndCache();

        balanceRoles();
    }

    public double getXpBooster() {
        removeExpiredBoosters();
        return xpBooster;
    }

    public void setXpBooster(double xpBooster) {
        this.xpBooster = xpBooster;

        mongoProfile.put("xpbooster", xpBooster);
        updateMongoProfileAndCache();
    }

    public double getCoinBooster() {
        removeExpiredBoosters();
        return coinBooster;
    }

    public void setCoinBooster(double coinBooster) {
        this.coinBooster = coinBooster;

        mongoProfile.put("coinbooster", coinBooster);
        updateMongoProfileAndCache();
    }

    // Setters

    public int getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(int dailyStreak) {
        this.dailyStreak = dailyStreak;

        mongoProfile.put("dailystreak", dailyStreak);
        updateMongoProfileAndCache();
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(@NotNull String github) {
        this.github = github;

        mongoProfile.put("github", github);
        updateMongoProfileAndCache();
    }

    public String getBio() {
        return bio;
    }

    public void setBio(@NotNull String bio) {
        this.bio = bio;

        mongoProfile.put("bio", bio);
        updateMongoProfileAndCache();
    }

    public String getOldNickname() {
        return oldNickname;
    }

    public void setOldNickname(@NotNull String oldNickname) {
        this.oldNickname = oldNickname;

        mongoProfile.put("previousNickname", oldNickname);
        updateMongoProfileAndCache();
    }

    public Document getCooldowns() {
        return cooldowns;
    }

    public void setCooldowns(@NotNull Document cooldowns) {
        this.cooldowns = cooldowns;

        mongoProfile.put("cooldowns", cooldowns);
        updateMongoProfileAndCache();
    }

    public ArrayList<String> getBadges() {
        return badges;
    }

    public void setBadges(@NotNull ArrayList<String> badges) {
        this.badges = badges;

        mongoProfile.put("badges", badges);
        updateMongoProfileAndCache();
    }

    public int getXpRank() {
        return CACHE.xpRankCache.getRank(getId());
    }

    public int getCoinRank() {
        return CACHE.coinRankCache.getRank(getId());
    }

    // Extra Methods

    public boolean getCoinMessageToggle() {
        return coinMessageToggle;
    }

    public void setCoinMessageToggle(boolean coinMessageToggle) {
        this.coinMessageToggle = coinMessageToggle;

        mongoProfile.put("coinMessageToggle", coinMessageToggle);
        updateMongoProfileAndCache();
    }

    public void addXp(long xpToAdd) {
        setXp(getXp() + xpToAdd);
    }

    public void addCoins(long coinsToAdd) {
        setCoins(getCoins() + coinsToAdd);
    }

    public void refreshProfile() {
        SourceProfile profile = CACHE.getProfileById(getId());
        if (profile == null) return;

        insertProfileFromObject(profile);
    }

    public void balanceLevel() {
        long level = getLevel();
        long xp = getXp();

        long previousRequiredXP = level == 0 ? 0 : Utility.getAllPreviousLevels(level);
        long requiredXP = Utility.getXpFormula(level + 1) + previousRequiredXP;

        while (xp >= requiredXP) {
            previousRequiredXP = level == 0 ? 0 : Utility.getAllPreviousLevels(level);
            requiredXP = Utility.getXpFormula(level + 1) + previousRequiredXP;

            // Redundancy check is required, unless one level is subtracted before setting level
            if (xp >= requiredXP) {
                level += 1;
            }
        }

        setLevel(level);
    }

    private void balanceRoles() {
        Member member = Utility.getMemberByIdentifier(GUILD, user.getId());

        if (member == null) return;

        List<Role> memberRoles = member.getRoles();
        if (memberRoles.contains(MVP) && memberRoles.contains(VIP)) return;

        long vipLevel = Utility.vipLevel;
        long mvpLevel = Utility.mvpLevel;


        if (!memberRoles.contains(MVP) && getLevel() >= mvpLevel) {
            GUILD.addRoleToMember(member, MVP).reason("Reached MVP Level").queue();
        }

        if (!memberRoles.contains(VIP) && getLevel() >= vipLevel) {
            GUILD.addRoleToMember(member, VIP).reason("Reached VIP Level").queue();
        }
    }

    public void deleteProfile() {
        userData.deleteMany(getMongoProfile());
        CACHE.removeProfile(this);
    }

    private void removeExpiredBoosters() {
        Document cooldowns = getCooldowns();

        if (coinBooster > 1) {
            long coinExpiration = cooldowns.containsKey("coinbooster") ? cooldowns.getLong("coinbooster") : 0;

            if (coinExpiration <= System.currentTimeMillis()) {
                cooldowns.remove("coinbooster");
                setCoinBooster(1);
            }
        }

        if (xpBooster > 1) {
            long xpExpiration = cooldowns.containsKey("xpbooster") ? cooldowns.getLong("xpbooster") : 0;

            if (xpBooster > 1 && xpExpiration <= System.currentTimeMillis()) {
                cooldowns.remove("xpbooster");
                setXpBooster(1);
            }
        }

        if (cooldowns != getCooldowns()) {
            setCooldowns(cooldowns);
        }
    }

    // Mongo Methods

    private void setRank(int rank) {
        if (rank == this.rank) return;
        this.rank = rank;

        mongoProfile.put("rank", rank);
        updateMongoProfileAndCache();
    }

    private void insertProfileFromObject(SourceProfile profile) {
        mongoProfile = profile.getMongoProfile();

        id = profile.getId();
        name = profile.getName();
        oldNickname = profile.getOldNickname();

        xp = profile.getXp();
        coins = profile.getCoins();
        level = profile.getLevel();
        rank = profile.getXpRank();

        xpBooster = profile.getXpBooster();
        coinBooster = profile.getCoinBooster();

        dailyStreak = profile.getDailyStreak();

        github = profile.getGithub();
        bio = profile.getBio();
        coinMessageToggle = profile.getCoinMessageToggle();

        cooldowns = profile.getCooldowns();
        badges = profile.getBadges();

        user = profile.getUser();
    }

    private void insertProfileFromDocument(Document mongoProfile) {
        if (mongoProfile == null) mongoProfile = createMongoProfile();

        this.mongoProfile = mongoProfile;

        id = mongoProfile.getString("id");
        name = mongoProfile.getString("name");
        oldNickname = mongoProfile.containsKey("previousNickname") ? mongoProfile.getString("previousNickname") : "N/A";

        xp = mongoProfile.getLong("xp");
        coins = mongoProfile.getLong("coins");
        level = mongoProfile.getLong("level");

        rank = mongoProfile.getInteger("rank");

        xpBooster = mongoProfile.getDouble("xpbooster");
        coinBooster = mongoProfile.getDouble("coinbooster");

        dailyStreak = mongoProfile.getInteger("dailystreak");

        github = mongoProfile.containsKey("github") ? mongoProfile.getString("github") : "";
        bio = mongoProfile.containsKey("bio") ? mongoProfile.getString("bio") : "";

        coinMessageToggle = mongoProfile.containsKey("coinMessageToggle") ?
                mongoProfile.getBoolean("coinMessageToggle") : true;

        cooldowns = (Document) mongoProfile.get("cooldowns");
        badges = mongoProfile.containsKey("badges") ? (ArrayList<String>) mongoProfile.get("badges") : new ArrayList<>();

        try {
            user = GUILD.getMemberById(id).getUser();
        } catch (NullPointerException ignored) {

        }
    }

    // Static methods

    @NotNull
    private Document createMongoProfile() {
        Document newMongoProfile = new Document("id", getId())
                .append("name", user.getName())
                .append("xp", 0L)
                .append("coins", 0L)
                .append("level", 0L)
                .append("rank", (int) (userData.countDocuments() + 1))
                .append("xpbooster", 1.0)
                .append("coinbooster", 1.0)
                .append("dailystreak", 0)
                .append("github", "")
                .append("bio", "")
                .append("coinMessageToggle", true)
                .append("cooldowns", new Document())
                .append("badges", new ArrayList<String>())
                .append("vouches", new Document());

        userData.insertOne(newMongoProfile);
        return newMongoProfile;
    }

    private void updateMongoProfileAndCache() {
        CACHE.updateProfile(this);
        Document oldProfile = queryProfile();

        if (oldProfile == null) return;
        if (oldProfile == mongoProfile) return;

        userData.updateOne(oldProfile, new Document("$set", mongoProfile));

    }

    @Nullable
    private Document queryProfile() {
        Document query = new Document().append("id", getId());
        return userData.find(query).first();
    }

    public enum CacheType {
        XP, COIN
    }

    private static final class Cache {

        private final Map<String, SourceProfile> profileCache = new ConcurrentHashMap<>();
        private final XpCache xpRankCache = new XpCache();
        private final CoinCache coinRankCache = new CoinCache();

        @Nullable
        SourceProfile getProfileById(@NotNull String id) {
            return profileCache.get(id);
        }

        void updateProfile(@NotNull SourceProfile profile) {
            profileCache.replace(profile.getId(), profile);
        }

        void insertProfile(@NotNull SourceProfile profile) {
            profileCache.put(profile.getId(), profile);
            xpRankCache.insertUserToLbCache(profile);
            coinRankCache.insertUserToLbCache(profile);
        }

        void removeProfile(@NotNull SourceProfile profile) {
            String id = profile.getId();

            profileCache.remove(id);
            xpRankCache.removeUser(profile);
            coinRankCache.removeUser(profile);
        }


        private static final class XpCache extends LeaderboardCache {

            void insertUserToLbCache(@NotNull SourceProfile profile) {
                insertUserToLbCacheInternal(profile.getId(), profile.getXp());
            }

            void cacheUserRanks() {
                cacheUserRanksInternal("xp");
            }

            void updateUser(@NotNull SourceProfile profile) {
                updateUserInternal(profile.getId(), profile.getXp());
            }
        }

        private static final class CoinCache extends LeaderboardCache {

            void insertUserToLbCache(@NotNull SourceProfile profile) {
                insertUserToLbCacheInternal(profile.getId(), profile.getCoins());
            }

            void cacheUserRanks() {
                cacheUserRanksInternal("coins");
            }

            void updateUser(@NotNull SourceProfile profile) {
                updateUserInternal(profile.getId(), profile.getCoins());
            }
        }

        private abstract static class LeaderboardCache {

            protected LinkedHashMap<String, Long> lbCache = new LinkedHashMap<>();

            void removeUser(@NotNull SourceProfile profile) {
                lbCache.remove(profile.getId());
                sortLeaderboard();
            }

            Integer getRank(@NotNull String id) {
                AtomicInteger rankCount = new AtomicInteger(0);

                for (String userId : lbCache.keySet()) {
                    int rank = rankCount.incrementAndGet();
                    if (userId.equalsIgnoreCase(id)) {
                        return rank;
                    }
                }

                return lbCache.size() + 1;
            }


            void sortLeaderboard() {
                /*
                Gets the rankCache keySet, adds it to a linked list, sorts it by xp,
                then reverses it because the xp is ascending and not descending
                */
                LinkedList<Map.Entry<String, Long>> lbInfoList = new LinkedList<>(lbCache.entrySet());
                lbInfoList.sort(Map.Entry.comparingByValue());
                Collections.reverse(lbInfoList);

                LinkedHashMap<String, Long> sortedMap = new LinkedHashMap<>();

                lbInfoList.forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

                lbCache.clear();
                lbCache = sortedMap;
            }

            LinkedHashMap<String, Integer> getLbByPage(int page) {
                LinkedHashMap<String, Integer> ranks = new LinkedHashMap<>();

                lbCache.keySet().stream()
                        .skip(page == 1 ? 0 : (page * 10) - 10)
                        .limit(10)
                        .forEach(id -> ranks.put(id, getRank(id)));

                return ranks;
            }

            protected void cacheUserRanksInternal(String propertyName) {
                userData.find().forEach((Block<? super Document>) document -> {
                    String id = document.getString("id");

                    if (GUILD.getMemberById(id) == null) {
                        userData.deleteOne(document);
                        return;
                    }

                    long xp = document.getLong(propertyName);

                    if (!lbCache.containsKey(id)) {
                        lbCache.put(id, xp);
                    }


                });
            }

            protected void insertUserToLbCacheInternal(String id, long value) {
                if (!lbCache.containsKey(id)) {
                    lbCache.put(id, value);
                }
                sortLeaderboard();
            }

            protected void updateUserInternal(String id, long value) {
                try {
                    lbCache.remove(id);
                    lbCache.put(id, value);
                    sortLeaderboard();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}