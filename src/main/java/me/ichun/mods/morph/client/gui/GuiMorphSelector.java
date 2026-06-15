package me.ichun.mods.morph.client.gui;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketMorphRequest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
public class GuiMorphSelector extends Screen {
    private int selectedIndex = 0;
    public GuiMorphSelector() { super(Component.literal("Morph Selector")); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, "Morph Selector", width / 2, 10, 0xFFFFFF);
        PlayerMorphData data = MorphHandler.INSTANCE.getPlayerMorphData(minecraft.player);
        if (data != null) {
            List<MorphVariant> morphs = data.acquiredMorphs;
            for (int i = 0; i < morphs.size(); i++) {
                int color = (i == selectedIndex) ? 0xFFFF00 : 0xFFFFFF; String name = morphs.get(i).id.toString(); if (i == selectedIndex) name = "> " + name + " <";
                graphics.drawCenteredString(font, name, width / 2, 40 + i * 12, color);
            }
        }
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PlayerMorphData data = MorphHandler.INSTANCE.getPlayerMorphData(minecraft.player); if (data == null) return super.keyPressed(keyCode, scanCode, modifiers);
        List<MorphVariant> morphs = data.acquiredMorphs;
        if (keyCode == 256) { this.onClose(); return true; }
        else if (keyCode == 265) { selectedIndex = (selectedIndex - 1 + morphs.size()) % Math.max(1, morphs.size()); return true; }
        else if (keyCode == 264) { selectedIndex = (selectedIndex + 1) % Math.max(1, morphs.size()); return true; }
        else if (keyCode == 257) { if (!morphs.isEmpty()) { Morph.channel.sendToServer(new PacketMorphRequest(morphs.get(selectedIndex).id.toString())); this.onClose(); } return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
