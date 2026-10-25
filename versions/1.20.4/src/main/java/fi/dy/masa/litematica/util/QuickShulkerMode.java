package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum QuickShulkerMode implements IConfigOptionListEntry
{
    CLICK_SLOT  ("click_slot",  "litematica.gui.label.quick_shulker_mode.click_slot"),
    INVOKE      ("invoke",      "litematica.gui.label.quick_shulker_mode.invoke");

    public static final ImmutableList<QuickShulkerMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    QuickShulkerMode(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public QuickShulkerMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static QuickShulkerMode fromStringStatic(String name)
    {
        for (QuickShulkerMode val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return QuickShulkerMode.INVOKE;
    }
}
