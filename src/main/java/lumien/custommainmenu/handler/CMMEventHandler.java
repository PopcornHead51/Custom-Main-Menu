package lumien.custommainmenu.handler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lumien.custommainmenu.CustomMainMenu;
import lumien.custommainmenu.configuration.elements.Panorama;
import lumien.custommainmenu.gui.GuiCustom;
import lumien.custommainmenu.gui.GuiCustomButton;
import lumien.custommainmenu.gui.GuiCustomWrappedButton;
import lumien.custommainmenu.gui.GuiFakeMain;

public class CMMEventHandler {

    public long displayMs = -1L;
    Field guiField;
    GuiCustom actualGui;

    public CMMEventHandler() {
        try {
            this.guiField = GuiScreenEvent.class.getDeclaredField("gui");
            this.guiField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void openGui(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu) {
            GuiCustom customMainMenu = CustomMainMenu.INSTANCE.config.getGUI("mainmenu");
            if (customMainMenu != null) {
                CMMEventHandler.refreshPanorama(customMainMenu);
                event.gui = customMainMenu;
            }
        } else if (event.gui instanceof GuiCustom custom) {
            GuiCustom target = CustomMainMenu.INSTANCE.config.getGUI(custom.guiConfig.name);
            CMMEventHandler.refreshPanorama(target == null ? custom : target);
            if (target != null && target != custom) {
                event.gui = target;
            }
        }
    }

    /**
     * Picks the panorama for the dimension the player was last in, and rolls a new random set if several are
     * configured. Done here rather than in initGui so that resizing the window doesn't change the panorama.
     */
    private static void refreshPanorama(GuiCustom gui) {
        if (gui == null || gui.guiConfig.panorama == null) {
            return;
        }
        Panorama panorama = gui.guiConfig.panorama;
        if (panorama.synced && !gui.guiConfig.name.equals("mainmenu")) {
            GuiCustom mainMenu = CustomMainMenu.INSTANCE.config.getGUI("mainmenu");
            if (mainMenu != null && mainMenu != gui && mainMenu.guiConfig.panorama != null) {
                panorama.copyFrom(mainMenu.guiConfig.panorama);
                return;
            }
        }
        panorama.reroll();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void initGuiPostEarly(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiCustom custom) {
            if (custom.guiConfig.name.equals("mainmenu")) {
                event.buttonList = new ArrayList<>();
                this.actualGui = custom;
                try {
                    this.guiField.set(event, new GuiFakeMain());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void initGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiFakeMain) {
            HashMap<Integer, GuiButton> removedButtons = new HashMap<>();
            // noinspection unchecked
            Iterator<GuiButton> iterator = event.buttonList.iterator();
            while (iterator.hasNext()) {
                GuiButton b = iterator.next();
                if (b instanceof GuiCustomButton) continue;
                iterator.remove();
                removedButtons.put(b.id, b);
                if (b.id == 666 && Loader.isModLoaded("OpenEye")) {
                    CustomMainMenu.INSTANCE.logger.log(
                            Level.DEBUG,
                            "Found OpenEye button, use a wrapped button to config this. (" + b.id + ")");
                    continue;
                }
                if (b.id == 404 && Loader.isModLoaded("VersionChecker")) {
                    CustomMainMenu.INSTANCE.logger.log(
                            Level.DEBUG,
                            "Found VersionChecker button, use a wrapped button to config this. (" + b.id + ")");
                    continue;
                }
                CustomMainMenu.INSTANCE.logger.log(
                        Level.DEBUG,
                        "Found unsupported button, use a wrapped button to config this. (" + b.id + ")");
            }
            for (GuiButton o : this.actualGui.getButtonList()) {
                if (!(o instanceof GuiCustomWrappedButton b)) continue;
                CustomMainMenu.INSTANCE.logger.log(
                        Level.DEBUG,
                        "Initiating Wrapped Button " + b.wrappedButtonID
                                + " with "
                                + removedButtons.get(b.wrappedButtonID));
                b.init(removedButtons.get(b.wrappedButtonID));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void renderScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (this.displayMs != -1L) {
            if (System.currentTimeMillis() - this.displayMs < 5000L) {
                Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
                        "Error loading config file, see console for more information",
                        0,
                        80,
                        16711680);
            } else {
                this.displayMs = -1L;
            }
        }
    }
}
