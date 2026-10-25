package fi.dy.masa.litematica.util;

import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;

/**
 * Quick Shulker.
 *
 * Easy Place needs the schematic block in the hand, and when it is not in the inventory at all
 * it can be pulled out of a shulker box that sits in the player inventory: the box is opened,
 * the wanted stack is moved out of it and the screen is closed again. The placement itself then
 * happens on the following clicks of the normal Easy Place handling.
 *
 * Opening a container that is not placed in the world is not something the vanilla server does
 * on its own, hence the two ways to reach the server.
 */
public class QuickShulkerUtils
{
    private static final String QUICK_SHULKER_MOD_ID = "quickshulker";
    private static final String QUICK_SHULKER_CLIENT_UTIL = "net.kyrptonaught.quickshulker.client.ClientUtil";
    // 服务端没有对应的插件/模组时，右键会把潜影盒拿进光标。等这么多刻还没有界面打开就把盒子放回去。
    private static final int RECOVERY_DELAY = 10;
    private static final int OPEN_TIMEOUT = 30;

    private static int cooldown;
    private static int pendingSlot = -1;
    private static int ticks;
    private static boolean waitingOpen = true;
    private static ItemStack needed = ItemStack.EMPTY;
    private static boolean invokeUnavailable;

    /**
     * Called when the Easy Place handling cannot find the wanted stack in the inventory.
     *
     * @return true when a restock sequence has been started or is already running
     */
    public static boolean requestRestock(Minecraft mc, ItemStack stack)
    {
        if (Configs.Generic.QUICK_SHULKER.getBooleanValue() == false ||
            stack.isEmpty() || cooldown > 0 || needed.isEmpty() == false)
        {
            return needed.isEmpty() == false;
        }

        // 只在没有打开任何界面时进行，潜影盒的点击发生在玩家物品栏的容器上
        if (mc.player == null || mc.player.containerMenu != mc.player.inventoryMenu ||
            mc.player.containerMenu.getCarried().isEmpty())
        {
            return false;
        }

        int slot = InventoryUtils.findSlotWithBoxWithItem(mc.player.inventoryMenu, stack, false);

        if (slot == -1)
        {
            return false;
        }

        QuickShulkerMode mode = (QuickShulkerMode) Configs.Generic.QUICK_SHULKER_MODE.getOptionListValue();

        if (mode == QuickShulkerMode.INVOKE)
        {
            if (invokeQuickShulker(mc.player.inventoryMenu.slots.get(slot).getItem(), slot) == false)
            {
                return false;
            }
        }
        else
        {
            QuickShulkerClicks.send(mc, slot, 1);
        }

        pendingSlot = slot;
        needed = stack.copy();
        ticks = 0;
        cooldown = Configs.Generic.QUICK_SHULKER_COOLDOWN.getIntegerValue();

        return true;
    }

    public static void tick()
    {
        if (cooldown > 0)
        {
            cooldown--;
        }

        if (needed.isEmpty())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            reset();
            return;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;

        if (waitingOpen == false)
        {
            // 潜影盒界面已经打开，把需要的方块搬出来
            if (menu == mc.player.inventoryMenu)
            {
                reset();
                return;
            }

            if (++ticks > OPEN_TIMEOUT)
            {
                close(mc);
                reset();
                return;
            }

            int slot = findItemInContainer(mc, menu);

            if (slot != -1)
            {
                QuickShulkerClicks.sendQuickMove(mc, slot);
                close(mc);
                reset();
            }

            return;
        }

        // 等待服务端打开潜影盒
        if (menu != mc.player.inventoryMenu)
        {
            waitingOpen = false;
            ticks = 0;
            return;
        }

        if (++ticks >= RECOVERY_DELAY && menu.getCarried().isEmpty() == false)
        {
            // 服务端没有这个功能，右键把潜影盒拿进了光标，放回去
            QuickShulkerClicks.send(mc, pendingSlot, 0);
            reset();
            return;
        }

        if (ticks >= OPEN_TIMEOUT)
        {
            reset();
        }
    }

    private static int findItemInContainer(Minecraft mc, AbstractContainerMenu menu)
    {
        for (Slot slot : menu.slots)
        {
            if (slot.container == mc.player.getInventory())
            {
                continue;
            }

            ItemStack stack = slot.getItem();

            if (stack.isEmpty() == false && ItemStack.isSameItemSameComponents(needed, stack))
            {
                return slot.index;
            }
        }

        return -1;
    }

    private static void close(Minecraft mc)
    {
        if (mc.player != null)
        {
            mc.player.closeContainer();
        }
    }

    private static void reset()
    {
        needed = ItemStack.EMPTY;
        pendingSlot = -1;
        ticks = 0;
        waitingOpen = true;
    }

    /**
     * The Quick Shulker mod does the click itself, but also knows the mod specific items it
     * supports. Calling it keeps both sides in sync, but it needs the mod to be installed.
     */
    private static boolean invokeQuickShulker(ItemStack stack, int slot)
    {
        if (FabricLoader.getInstance().isModLoaded(QUICK_SHULKER_MOD_ID) == false)
        {
            logUnavailable("the mod is not installed");

            return false;
        }

        try
        {
            Class<?> util = Class.forName(QUICK_SHULKER_CLIENT_UTIL);

            for (Method method : util.getMethods())
            {
                if (method.getName().equalsIgnoreCase("CheckAndSend") && method.getParameterCount() == 2 &&
                    method.getParameterTypes()[0] == ItemStack.class && method.getParameterTypes()[1] == int.class)
                {
                    return Boolean.TRUE.equals(method.invoke(null, stack, slot));
                }
            }

            logUnavailable("no compatible entry point was found");

            return false;
        }
        catch (Throwable t)
        {
            logUnavailable(t.toString());

            return false;
        }
    }

    private static void logUnavailable(String reason)
    {
        if (invokeUnavailable == false)
        {
            invokeUnavailable = true;
            Litematica.LOGGER.warn("Quick Shulker: the invoke mode is unavailable ({}), use the click mode instead", reason);
        }
    }
}
