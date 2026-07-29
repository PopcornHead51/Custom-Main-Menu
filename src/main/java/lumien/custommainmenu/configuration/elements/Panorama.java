package lumien.custommainmenu.configuration.elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import lumien.custommainmenu.CustomMainMenu;
import lumien.custommainmenu.configuration.GuiConfig;
import lumien.custommainmenu.handler.DimensionTracker;
import lumien.custommainmenu.lib.DimensionInfo;
import lumien.custommainmenu.lib.textures.ITexture;
import lumien.custommainmenu.lib.textures.TextureResourceLocation;

/**
 * A panorama is described by one or more {@link PanoramaSet}s. Each set carries a texture path containing the
 * placeholder <code>%c</code>, which is substituted with the cube face index 0-5, plus its own optional gradient
 * setting.
 * <p>
 * Two further placeholders are resolved against the dimension the player was last in:
 * <ul>
 * <li><code>%d</code> - the numeric dimension id, e.g. <code>-1</code></li>
 * <li><code>%n</code> - the sanitized dimension name, e.g. <code>nether</code></li>
 * </ul>
 * Sets can also be registered per dimension id explicitly, and when more than one set is available for the current
 * dimension a new one is rolled every time the menu is opened.
 */
public class Panorama extends Element {

    /** Used when a config supplies no usable images at all. */
    private static final PanoramaSet VANILLA_SET = new PanoramaSet(
            "minecraft:textures/gui/title/background/panorama_%c.png",
            null);

    private final List<PanoramaSet> defaultSets;
    private final HashMap<Integer, DimensionSet> dimensionSets;
    /** Fully substituted pattern -> the six faces, so URL panoramas aren't re-downloaded on every menu open. */
    private final HashMap<String, ITexture[]> resolveCache;
    private final Random rand;

    public final boolean blur;
    /** Fallback for sets that don't carry a gradient setting of their own. */
    public final boolean gradient;
    public boolean animate;
    public boolean synced;
    /** Whether a new set is rolled every time the menu is entered. Defaults to true when several sets are given. */
    public boolean random;
    public int position;
    public int animationSpeed;

    /** The six faces currently in use. Never null once {@link #reroll()} has run. */
    public ITexture[] locations;
    private PanoramaSet currentSet;
    private boolean currentGradient;

    public Panorama(GuiConfig parent, List<PanoramaSet> sets, boolean blur, boolean gradient) {
        super(parent);
        this.defaultSets = new ArrayList<>();
        if (sets != null) {
            for (PanoramaSet set : sets) {
                if (set != null && set.pattern != null && !set.pattern.isEmpty()) {
                    this.defaultSets.add(set);
                }
            }
        }
        if (this.defaultSets.isEmpty()) {
            this.defaultSets.add(VANILLA_SET);
        }
        this.dimensionSets = new HashMap<>();
        this.resolveCache = new HashMap<>();
        this.rand = new Random();
        this.blur = blur;
        this.gradient = gradient;
        this.currentGradient = gradient;
        this.animate = true;
        this.animationSpeed = 1;
        this.synced = false;
        this.random = this.defaultSets.size() > 1;
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setAnimationSpeed(int animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    /**
     * Registers the sets to use when the player was last in the given dimension.
     *
     * @param randomOverride null to inherit the panorama wide "random" setting.
     */
    public void addDimensionSet(int dimensionId, List<PanoramaSet> sets, Boolean randomOverride) {
        if (sets == null || sets.isEmpty()) {
            return;
        }
        this.dimensionSets.put(dimensionId, new DimensionSet(sets, randomOverride));
    }

    public boolean hasDimensionSets() {
        return !this.dimensionSets.isEmpty();
    }

    /**
     * Lazily resolves the faces. Resolving can't happen while the config is being parsed because resource packs aren't
     * mounted during preInit yet.
     */
    public ITexture[] getLocations() {
        if (this.locations == null) {
            this.reroll();
        }
        return this.locations;
    }

    /**
     * @return whether the darkening gradient should be drawn over the set that's currently showing.
     */
    public boolean drawGradient() {
        if (this.locations == null) {
            this.reroll();
        }
        return this.currentGradient;
    }

    public PanoramaSet getCurrentSet() {
        return this.currentSet;
    }

    /**
     * Picks the set to display. Call this whenever the menu is entered.
     */
    public void reroll() {
        DimensionInfo info = DimensionTracker.getLastDimension();
        List<PanoramaSet> candidates = new ArrayList<>();

        if (info != null) {
            DimensionSet dimensionSet = this.dimensionSets.get(info.getDimensionId());
            if (dimensionSet != null) {
                boolean shuffle = dimensionSet.random == null ? this.random : dimensionSet.random;
                candidates.addAll(order(dimensionSet.sets, shuffle, this.rand));
            }
        }
        candidates.addAll(order(this.defaultSets, this.random, this.rand));
        candidates.add(VANILLA_SET);

        for (PanoramaSet set : candidates) {
            ITexture[] resolved = this.resolve(set.pattern, info, true);
            if (resolved == null) continue;
            this.apply(set, resolved);
            return;
        }

        // Nothing passed the existence check, so bind the first usable candidate anyway and let Minecraft show its
        // missing texture rather than crashing on a null face.
        for (PanoramaSet set : candidates) {
            ITexture[] resolved = this.resolve(set.pattern, info, false);
            if (resolved == null) continue;
            CustomMainMenu.INSTANCE.logger
                    .log(Level.WARN, "No panorama textures could be found, falling back to " + set.pattern);
            this.apply(set, resolved);
            return;
        }
    }

    private void apply(PanoramaSet set, ITexture[] resolved) {
        this.locations = resolved;
        this.currentSet = set;
        this.currentGradient = set.gradient == null ? this.gradient : set.gradient;
    }

    /**
     * Adopts the set another panorama is currently showing, used by GUIs configured with "synced". The gradient comes
     * along with it, otherwise a synced GUI could darken a panorama the main menu is showing untouched.
     */
    public void copyFrom(Panorama other) {
        if (other == null || other == this) {
            return;
        }
        this.locations = other.getLocations();
        this.currentSet = other.currentSet;
        this.currentGradient = other.currentGradient;
    }

    private static List<PanoramaSet> order(List<PanoramaSet> sets, boolean shuffle, Random rand) {
        List<PanoramaSet> copy = new ArrayList<>(sets);
        if (shuffle && copy.size() > 1) {
            Collections.shuffle(copy, rand);
        }
        return copy;
    }

    /**
     * @return the six faces, or null if the pattern can't be used (unresolvable placeholder or missing texture).
     */
    private ITexture[] resolve(String pattern, DimensionInfo info, boolean checkExistence) {
        String base = pattern;
        if (base.contains("%d") || base.contains("%n")) {
            if (info == null) {
                // The player has never been in a world, so there's nothing to substitute.
                return null;
            }
            base = base.replace("%d", Integer.toString(info.getDimensionId()))
                    .replace("%n", info.getSanitizedDimensionName());
        }

        ITexture[] cached = this.resolveCache.get(base);
        if (cached != null) {
            return cached;
        }

        ITexture[] faces = new ITexture[6];
        for (int i = 0; i < 6; ++i) {
            String path = base.replace("%c", Integer.toString(i));
            ITexture texture = GuiConfig.getWantedTexture(path);
            if (checkExistence && texture instanceof TextureResourceLocation && !exists((ResourceLocation) texture)) {
                return null;
            }
            faces[i] = texture;
        }
        this.resolveCache.put(base, faces);
        return faces;
    }

    private static boolean exists(ResourceLocation location) {
        try {
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            if (resource != null) {
                IOUtils.closeQuietly(resource.getInputStream());
            }
            return true;
        } catch (IOException e) {
            return false;
        } catch (Throwable t) {
            // Resource manager not ready, assume the texture is fine rather than dropping the whole set.
            return true;
        }
    }

    /**
     * One selectable panorama: the image pattern plus the settings that belong to that specific set of images rather
     * than to the panorama as a whole.
     */
    public static class PanoramaSet {

        public final String pattern;
        /** null means "use the panorama wide gradient setting". */
        public final Boolean gradient;

        public PanoramaSet(String pattern, Boolean gradient) {
            this.pattern = pattern;
            this.gradient = gradient;
        }

        @Override
        public String toString() {
            return this.pattern + (this.gradient == null ? "" : " (gradient=" + this.gradient + ")");
        }
    }

    private static class DimensionSet {

        final List<PanoramaSet> sets;
        final Boolean random;

        DimensionSet(List<PanoramaSet> sets, Boolean random) {
            this.sets = new ArrayList<>(sets);
            this.random = random;
        }
    }
}
