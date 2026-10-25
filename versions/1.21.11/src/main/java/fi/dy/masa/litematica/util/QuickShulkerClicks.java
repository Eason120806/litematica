package fi.dy.masa.litematica.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

/**
 * Sends one slot click without predicting it locally, so the answer from the server is what
 * decides whether the click picked something up or opened a screen.
 */
public final class QuickShulkerClicks
{
    private QuickShulkerClicks() { }

    public static void send(Minecraft mc, int slot, int button)
    {
        send(mc, slot, button, ClickType.PICKUP);
    }

    public static void sendQuickMove(Minecraft mc, int slot)
    {
        send(mc, slot, 0, ClickType.QUICK_MOVE);
    }

    private static void send(Minecraft mc, int slot, int button, ClickType type)
    {
        ClientPacketListener connection = mc.getConnection();

        if (connection == null || mc.player == null)
        {
            return;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;

        connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(),
                (short) slot, (byte) button, type, new Int2ObjectOpenHashMap<>(),
                HashedStack.EMPTY));
    }
}
