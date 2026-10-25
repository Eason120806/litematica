package fi.dy.masa.litematica.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Sends one slot click without predicting it locally, so the answer from the server is what
 * decides whether the click picked something up or opened a screen.
 */
public final class QuickShulkerClicks
{
    private QuickShulkerClicks() { }

    public static void send(MinecraftClient mc, int slot, int button)
    {
        send(mc, slot, button, SlotActionType.PICKUP);
    }

    public static void sendQuickMove(MinecraftClient mc, int slot)
    {
        send(mc, slot, 0, SlotActionType.QUICK_MOVE);
    }

    private static void send(MinecraftClient mc, int slot, int button, SlotActionType type)
    {
        ClientPlayNetworkHandler connection = mc.getNetworkHandler();

        if (connection == null || mc.player == null)
        {
            return;
        }

        ScreenHandler menu = mc.player.currentScreenHandler;

        connection.sendPacket(new ClickSlotC2SPacket(menu.syncId, menu.getRevision(),
                slot, button, type, menu.getCursorStack().copy(), new Int2ObjectOpenHashMap<>()));
    }
}
