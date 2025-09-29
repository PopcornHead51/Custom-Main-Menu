package lumien.custommainmenu.lib.actions;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.gui.stream.GuiStreamOptions;
import net.minecraft.client.gui.stream.GuiStreamUnavailable;
import net.minecraft.client.stream.IStream;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.Loader;
import lumien.custommainmenu.CustomMainMenu;
import lumien.custommainmenu.gui.GuiCustom;

public class ActionOpenGUI implements IAction {

    final String guiName;

    public ActionOpenGUI(String guiName) {
        this.guiName = guiName;
    }

    @Override
    public void perform(Object source, GuiCustom menu) {
        GuiScreen gui = null;
        if (this.guiName.startsWith("custom.")) {
            String customName = this.guiName.substring(7);
            gui = CustomMainMenu.INSTANCE.config.getGUI(customName);
        } else if (this.guiName.equalsIgnoreCase("mods")) {
            gui = new GuiModList(menu);
        } else if (this.guiName.equalsIgnoreCase("singleplayer")) {
            gui = new GuiSelectWorld(menu);
        } else if (this.guiName.equalsIgnoreCase("singleplayer.createworld")) {
            gui = new GuiCreateWorld(menu);
        } else if (this.guiName.equalsIgnoreCase("multiplayer")) {
            gui = new GuiMultiplayer(menu);
        } else if (this.guiName.equalsIgnoreCase("options")) {
            gui = new GuiOptions(menu, menu.mc.gameSettings);
        } else if (this.guiName.equalsIgnoreCase("languages")) {
            gui = new GuiLanguage(menu, menu.mc.gameSettings, menu.mc.getLanguageManager());
        } else if (this.guiName.equalsIgnoreCase("options.ressourcepacks")) {
            gui = new GuiScreenResourcePacks(menu);
        } else if (this.guiName.equalsIgnoreCase("options.resourcepacks")) {
            gui = new GuiScreenResourcePacks(menu);
        } else if (this.guiName.equalsIgnoreCase("options.shaderpacks")) {
            if (Loader.isModLoaded("angelica")) {
                try {
                    Class<?> guiShadersClass = Class.forName("net.coderbot.iris.gui.screen.ShaderPackScreen");
                    Constructor<?> constructor = guiShadersClass.getConstructor(GuiScreen.class);
                    Object guiInstance = constructor.newInstance(menu.mc.currentScreen);
                    gui = (GuiScreen) guiInstance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (Loader.isModLoaded("swansong")) {
                try {
                    Class<?> guiShadersClass = Class.forName("com.ventooth.swansong.gui.GuiShaders");
                    Constructor<?> constructor = guiShadersClass.getConstructor(GuiScreen.class);
                    Object guiInstance = constructor.newInstance(menu.mc.currentScreen);
                    gui = (GuiScreen) guiInstance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                gui = new GuiScreenResourcePacks(menu);
            }
        } else if (this.guiName.equalsIgnoreCase("options.snooper")) {
            gui = new GuiSnooper(menu, menu.mc.gameSettings);
        } else if (this.guiName.equalsIgnoreCase("options.sounds")) {
            gui = new GuiScreenOptionsSounds(menu, menu.mc.gameSettings);
        } else if (this.guiName.equalsIgnoreCase("options.broadcast")) {
            menu.mc.gameSettings.saveOptions();
            IStream istream = menu.mc.func_152346_Z();
            if (istream.func_152936_l() && istream.func_152928_D()) {
                gui = new GuiStreamOptions(menu, menu.mc.gameSettings);
            } else {
                GuiStreamUnavailable.func_152321_a(menu);
            }
        } else if (this.guiName.equalsIgnoreCase("options.video")) {
            if (Loader.isModLoaded("angelica")) {
                try {
                    Class<?> guiShadersClass = Class.forName("me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI");
                    Constructor<?> constructor = guiShadersClass.getConstructor(GuiScreen.class);
                    Object guiInstance = constructor.newInstance(menu.mc.currentScreen);
                    gui = (GuiScreen) guiInstance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (Loader.isModLoaded("notfine")) {
                try {
                    final String OPTION_PAGE_FQN = "me.jellysquid.mods.sodium.client.gui.options.OptionPage";
                    Class<?> optionPageClass = Class.forName(OPTION_PAGE_FQN);
                    Class<?> notFineOptionsClass = Class.forName("jss.notfine.gui.NotFineGameOptionPages");
                    Object generalPage = notFineOptionsClass.getMethod("general").invoke(null);
                    Object detailPage = notFineOptionsClass.getMethod("detail").invoke(null);
                    Object atmospherePage = notFineOptionsClass.getMethod("atmosphere").invoke(null);
                    Object particlesPage = notFineOptionsClass.getMethod("particles").invoke(null);
                    Object otherPage = notFineOptionsClass.getMethod("other").invoke(null);
                    Object[] subPages = (Object[]) Array.newInstance(optionPageClass, 4);
                    subPages[0] = detailPage;
                    subPages[1] = atmospherePage;
                    subPages[2] = particlesPage;
                    subPages[3] = otherPage;
                    Class<?> optionPageArrayClass = Class.forName("[L" + OPTION_PAGE_FQN + ";");
                    Constructor<?> constructor = Class.forName("jss.notfine.gui.GuiCustomMenu")
                            .getConstructor(GuiScreen.class, optionPageClass, optionPageArrayClass);
                    Object guiInstance = constructor.newInstance(menu.mc.currentScreen, generalPage, subPages);
                    gui = (GuiScreen) guiInstance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                gui = new GuiVideoSettings(menu, menu.mc.gameSettings);
            }
        } else if (this.guiName.equalsIgnoreCase("options.controls")) {
            gui = new GuiControls(menu, menu.mc.gameSettings);
        } else if (this.guiName.equalsIgnoreCase("options.multiplayer")) {
            gui = new ScreenChatOptions(menu, menu.mc.gameSettings);
        } else if (this.guiName.equalsIgnoreCase("mainmenu")) {
            gui = new GuiMainMenu();
        }
        if (gui != null) {
            Minecraft.getMinecraft().displayGuiScreen(gui);
        }
    }
}
