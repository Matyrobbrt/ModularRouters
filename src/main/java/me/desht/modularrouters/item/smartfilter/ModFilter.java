package me.desht.modularrouters.item.smartfilter;

import com.google.common.collect.Lists;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.client.util.ClientUtil;
import me.desht.modularrouters.container.AbstractSmartFilterMenu;
import me.desht.modularrouters.container.ModFilterMenu;
import me.desht.modularrouters.logic.filter.matchers.IItemMatcher;
import me.desht.modularrouters.logic.filter.matchers.ModMatcher;
import me.desht.modularrouters.network.messages.FilterSettingsMessage;
import me.desht.modularrouters.network.messages.GuiSyncMessage;
import me.desht.modularrouters.util.MFLocator;
import me.desht.modularrouters.util.ModNameCache;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ModFilter extends SmartFilterItem {
    private static final String NBT_MODS = "Mods";
    public static final int MAX_SIZE = 6;

    @Override
    public IItemMatcher compile(ItemStack filterStack, ItemStack moduleStack) {
        return new ModMatcher(getModList(filterStack));
    }

    public static List<String> getModList(ItemStack filterStack) {
        CompoundTag tag = filterStack.getTagElement(ModularRouters.MODID);
        if (tag != null) {
            ListTag items = tag.getList(NBT_MODS, Tag.TAG_STRING);
            List<String> res = Lists.newArrayListWithExpectedSize(items.size());
            for (int i = 0; i < items.size(); i++) {
                res.add(items.getString(i));
            }
            return res;
        } else {
            return Lists.newArrayList();
        }
    }

    private static void setModList(ItemStack filterStack, List<String> mods) {
        ListTag list = mods.stream().map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
        filterStack.getOrCreateTagElement(ModularRouters.MODID).put(NBT_MODS, list);
    }

    @Override
    public void addExtraInformation(ItemStack stack, List<Component> list) {
        super.addExtraInformation(stack, list);
        if (stack.getTagElement(ModularRouters.MODID) != null) {
            List<String> l = getModList(stack);
            list.add(ClientUtil.xlate("modularrouters.itemText.misc.filter.count", l.size()));
            list.addAll(l.stream()
                    .map(ModNameCache::getModName)
                    .map(s -> " \u2022 " + ChatFormatting.AQUA + s)
                    .map(Component::literal)
                    .toList());
        } else {
            list.add(ClientUtil.xlate("modularrouters.itemText.misc.filter.count", 0));
        }
    }

    @Override
    public AbstractSmartFilterMenu createMenu(int windowId, Inventory invPlayer, MFLocator loc) {
        return new ModFilterMenu(windowId, invPlayer, loc);
    }

    @Override
    public GuiSyncMessage onReceiveSettingsMessage(Player player, FilterSettingsMessage message, ItemStack filterStack, ItemStack moduleStack) {
        List<String> l;
        switch (message.op()) {
            case ADD_STRING -> {
                String modId = message.payload().getString("ModId");
                l = getModList(filterStack);
                if (l.size() < MAX_SIZE && !l.contains(modId)) {
                    l.add(modId);
                    setModList(filterStack, l);
                    return new GuiSyncMessage(filterStack);
                }
            }
            case REMOVE_AT -> {
                int pos = message.payload().getInt("Pos");
                l = getModList(filterStack);
                if (pos >= 0 && pos < l.size()) {
                    l.remove(pos);
                    setModList(filterStack, l);
                    return new GuiSyncMessage(filterStack);
                }
            }
            default -> ModularRouters.LOGGER.warn("received unexpected message type " + message.op() + " for " + filterStack);
        }
        return null;
    }

    @Override
    public int getSize(ItemStack filterStack) {
        CompoundTag tag = filterStack.getTagElement(ModularRouters.MODID);
        return tag != null ? tag.getList(NBT_MODS, Tag.TAG_STRING).size() : 0;
    }
}
