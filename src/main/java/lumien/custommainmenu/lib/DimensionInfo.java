package lumien.custommainmenu.lib;

import com.google.gson.JsonObject;

/**
 * Immutable snapshot of the world / dimension the player was last inside of.
 * <p>
 * This is written to <code>config/CustomMainMenu/lastdimension.json</code> so that it survives a game restart, which
 * means the very first main menu shown after launching the game already knows where the player left off.
 */
public class DimensionInfo {

    private final int dimensionId;
    private final String dimensionName;
    private final String worldName;
    private final boolean multiplayer;
    private final long lastPlayed;

    public DimensionInfo(int dimensionId, String dimensionName, String worldName, boolean multiplayer,
            long lastPlayed) {
        this.dimensionId = dimensionId;
        this.dimensionName = dimensionName == null ? "" : dimensionName;
        this.worldName = worldName == null ? "" : worldName;
        this.multiplayer = multiplayer;
        this.lastPlayed = lastPlayed;
    }

    public int getDimensionId() {
        return this.dimensionId;
    }

    /**
     * The raw name reported by the WorldProvider, e.g. "Overworld", "Nether", "The End".
     */
    public String getDimensionName() {
        return this.dimensionName;
    }

    /**
     * The dimension name lowercased with everything that isn't a letter or digit turned into an underscore, so it can
     * safely be used inside a ResourceLocation path. "The End" becomes "the_end".
     */
    public String getSanitizedDimensionName() {
        StringBuilder builder = new StringBuilder(this.dimensionName.length());
        for (int i = 0; i < this.dimensionName.length(); ++i) {
            char c = Character.toLowerCase(this.dimensionName.charAt(i));
            builder.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        }
        return builder.toString();
    }

    public String getWorldName() {
        return this.worldName;
    }

    public boolean isMultiplayer() {
        return this.multiplayer;
    }

    public long getLastPlayed() {
        return this.lastPlayed;
    }

    /**
     * Compares everything except the timestamp, so we only touch the disk when something actually changed.
     */
    public boolean sameLocation(DimensionInfo other) {
        return other != null && other.dimensionId == this.dimensionId
                && other.multiplayer == this.multiplayer
                && other.dimensionName.equals(this.dimensionName)
                && other.worldName.equals(this.worldName);
    }

    public JsonObject write() {
        JsonObject object = new JsonObject();
        object.addProperty("dimension", this.dimensionId);
        object.addProperty("dimensionName", this.dimensionName);
        object.addProperty("worldName", this.worldName);
        object.addProperty("multiplayer", this.multiplayer);
        object.addProperty("lastPlayed", this.lastPlayed);
        return object;
    }

    public static DimensionInfo read(JsonObject object) {
        if (object == null || !object.has("dimension")) {
            return null;
        }
        return new DimensionInfo(
                object.get("dimension").getAsInt(),
                object.has("dimensionName") ? object.get("dimensionName").getAsString() : "",
                object.has("worldName") ? object.get("worldName").getAsString() : "",
                object.has("multiplayer") && object.get("multiplayer").getAsBoolean(),
                object.has("lastPlayed") ? object.get("lastPlayed").getAsLong() : 0L);
    }

    @Override
    public String toString() {
        return "DimensionInfo[id=" + this.dimensionId
                + ", name="
                + this.dimensionName
                + ", world="
                + this.worldName
                + ", multiplayer="
                + this.multiplayer
                + "]";
    }
}
