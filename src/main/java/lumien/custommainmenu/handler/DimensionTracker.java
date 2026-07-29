package lumien.custommainmenu.handler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lumien.custommainmenu.CustomMainMenu;
import lumien.custommainmenu.lib.DimensionInfo;

/**
 * Keeps track of which dimension the player was last in, in both singleplayer and multiplayer, and remembers it across
 * restarts by writing it to disk.
 * <p>
 * There is no client side dimension change event on 1.7.10 - {@code PlayerEvent.PlayerChangedDimensionEvent} is only
 * fired by ServerConfigurationManager and never reaches a client that isn't hosting. What does happen on the client for
 * both the initial login and every later dimension change is that NetHandlerPlayClient builds a fresh WorldClient and
 * respawns the local player into it, so those two moments are what we listen for:
 * <ul>
 * <li>{@link WorldEvent.Load} with a remote world - fired from Minecraft#loadWorld for the incoming WorldClient</li>
 * <li>{@link EntityJoinWorldEvent} for the local player - covers respawns into an already loaded world</li>
 * </ul>
 * Both funnel into the same capture, which no-ops when nothing actually changed, so the double coverage is free.
 */
public class DimensionTracker {

    public static final DimensionTracker INSTANCE = new DimensionTracker();
    private static final String FILE_NAME = "lastdimension.json";

    private File file;
    private DimensionInfo lastDimension;

    private DimensionTracker() {}

    /**
     * Called from preInit on the client. Reads whatever was stored during the previous session.
     * <p>
     * This deliberately lives in a subfolder: ConfigurationLoader treats every .json directly inside
     * config/CustomMainMenu as a GUI definition, so a state file sitting next to mainmenu.json would be parsed as one
     * and blow up on the missing "buttons" section.
     */
    public void init(File configFolder) {
        File folder = new File(new File(configFolder, "CustomMainMenu"), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, FILE_NAME);
        File legacy = new File(new File(configFolder, "CustomMainMenu"), FILE_NAME);
        if (legacy.exists()) {
            // Left behind by an earlier build that wrote into the GUI config folder.
            if (!this.file.exists()) {
                legacy.renameTo(this.file);
            } else {
                legacy.delete();
            }
        }
        this.read();
    }

    /**
     * @return the dimension the player was last in, or null if we've never seen the player in a world.
     */
    public static DimensionInfo getLastDimension() {
        return INSTANCE.lastDimension;
    }

    /**
     * @return the last dimension id, or the supplied fallback if nothing has been recorded yet.
     */
    public static int getLastDimensionId(int fallback) {
        DimensionInfo info = INSTANCE.lastDimension;
        return info == null ? fallback : info.getDimensionId();
    }

    /**
     * Fired for the incoming WorldClient on login and on every dimension change. The isRemote check matters because the
     * integrated server loads its own worlds on the same event bus.
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world != null && event.world.isRemote) {
            this.capture(event.world);
        }
    }

    /**
     * EntityClientPlayerMP is only ever the local player, other players on a server are EntityOtherPlayerMP.
     */
    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityClientPlayerMP && event.world != null && event.world.isRemote) {
            this.capture(event.world);
        }
    }

    /**
     * Reads everything off the world that was handed to us rather than off Minecraft#theWorld, because during
     * WorldEvent.Load the client is still holding the previous world.
     */
    private void capture(World world) {
        if (world.provider == null) {
            return;
        }
        int dimensionId = world.provider.dimensionId;
        String dimensionName;
        try {
            dimensionName = world.provider.getDimensionName();
        } catch (Throwable t) {
            // Some dimension mods throw here if the provider isn't fully set up yet, it's only cosmetic.
            dimensionName = "";
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean multiplayer = !mc.isSingleplayer();
        String worldName = "";
        try {
            if (!multiplayer) {
                IntegratedServer server = mc.getIntegratedServer();
                if (server != null) {
                    worldName = server.getFolderName();
                }
            } else if (world.getWorldInfo() != null) {
                worldName = world.getWorldInfo().getWorldName();
            }
        } catch (Throwable t) {
            worldName = "";
        }
        DimensionInfo info = new DimensionInfo(
                dimensionId,
                dimensionName,
                worldName,
                multiplayer,
                System.currentTimeMillis());
        if (info.sameLocation(this.lastDimension)) {
            return;
        }
        this.lastDimension = info;
        CustomMainMenu.INSTANCE.logger.log(Level.DEBUG, "Recorded last dimension: " + info);
        this.write();
    }

    private void read() {
        if (this.file == null || !this.file.exists()) {
            return;
        }
        FileReader reader = null;
        try {
            reader = new FileReader(this.file);
            JsonObject object = new JsonParser().parse(reader).getAsJsonObject();
            this.lastDimension = DimensionInfo.read(object);
        } catch (Exception e) {
            CustomMainMenu.INSTANCE.logger
                    .log(Level.WARN, "Couldn't read " + FILE_NAME + ", starting without a last dimension.", e);
            this.lastDimension = null;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private void write() {
        if (this.file == null || this.lastDimension == null) {
            return;
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(this.file);
            writer.write(this.lastDimension.write().toString());
        } catch (Exception e) {
            CustomMainMenu.INSTANCE.logger.log(Level.WARN, "Couldn't write " + FILE_NAME + ".", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
