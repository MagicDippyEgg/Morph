package me.ichun.mods.morph.client.gui;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketMorphRequest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

public class GuiMorphSelector extends Screen {
    private int selectedIndex = 0;
    public GuiMorphSelector() { super(Component.literal("Morph Selector")); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        graphics.drawCenteredString(font, Component.translatable("gui.morph.title"), centerX, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.morph.help"), centerX, 22, 0xAAAAAA);

        PlayerMorphData data = MorphHandler.INSTANCE.getPlayerMorphData(minecraft.player);
        if (data != null) {
            List<MorphVariant> morphs = data.acquiredMorphs;
            int start = Math.max(0, selectedIndex - 5);
            int end = Math.min(morphs.size(), start + 11);

            for (int i = start; i < end; i++) {
                int color = (i == selectedIndex) ? 0xFFFF00 : 0xFFFFFF;
                MorphVariant variant = morphs.get(i);
                Component name;
                if (variant.id.getPath().equals("player")) {
                    name = Component.translatable("entity.minecraft.player");
                } else {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(variant.id);
                    name = (type != null) ? type.getDescription() : Component.literal(variant.id.toString());
                }

                String text = (i == selectedIndex) ? "> " + name.getString() + " <" : name.getString();
                graphics.drawCenteredString(font, text, centerX, 40 + (i - start) * 12, color);
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
