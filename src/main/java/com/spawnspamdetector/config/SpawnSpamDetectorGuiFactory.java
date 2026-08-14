package com.spawnspamdetector.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import com.spawnspamdetector.Tags;


public class SpawnSpamDetectorGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new SpawnSpamDetectorConfigGui(parentScreen);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    public static class SpawnSpamDetectorConfigGui extends GuiConfig {

        public SpawnSpamDetectorConfigGui(GuiScreen parentScreen) {
            super(
                parentScreen,
                getConfigElements(),
                Tags.MODID,
                false,
                false,
                I18n.format(Tags.MODID + ".config.title")
            );
        }

        private static List<IConfigElement> getConfigElements() {
            List<IConfigElement> elements = new ArrayList<>();
            elements.addAll(ConfigElement.from(SpawnSpamDetectorConfig.class).getChildElements());
            return elements;
        }
    }
}