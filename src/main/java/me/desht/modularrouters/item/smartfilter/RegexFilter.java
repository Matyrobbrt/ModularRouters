package me.desht.modularrouters.item.smartfilter;

import com.google.common.collect.Lists;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.logic.filter.matchers.IItemMatcher;
import me.desht.modularrouters.logic.filter.matchers.RegexMatcher;
import me.desht.modularrouters.network.messages.FilterSettingsMessage;
import me.desht.modularrouters.network.messages.GuiSyncMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class RegexFilter extends SmartFilterItem {
    private static final String NBT_REGEX = "Regex";
    public static final int MAX_SIZE = 6;

    @Override
    public boolean hasMenu() {
        return false;
    }

    public static List<String> getRegexList(ItemStack filterStack) {
        CompoundTag tag = filterStack.getTagElement(ModularRouters.MODID);
        if (tag != null) {
            ListTag items = tag.getList(NBT_REGEX, Tag.TAG_STRING);
            List<String> res = Lists.newArrayListWithExpectedSize(items.size());
            for (int i = 0; i < items.size(); i++) {
                res.add(items.getString(i));
            }
            return res;
        } else {
            return Lists.newArrayList();
        }
    }

    private static void setRegexList(ItemStack filterStack, List<String> regex) {
        ListTag list = regex.stream().map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
        filterStack.getOrCreateTagElement(ModularRouters.MODID).put(NBT_REGEX, list);
    }

    @Override
    public void addExtraInformation(ItemStack itemstack, List<Component> list) {
        super.addExtraInformation(itemstack, list);
        CompoundTag compound = itemstack.getTag();
        if (compound != null) {
            List<String> l = getRegexList(itemstack);
            addCountInfo(list, l.size());
            list.addAll(l.stream().map(s -> " • " + ChatFormatting.AQUA + "/" + s + "/").map(Component::literal).toList());
        } else {
            addCountInfo(list, 0);
        }
    }

    @Override
    public IItemMatcher compile(ItemStack filterStack, ItemStack moduleStack) {
        return new RegexMatcher(getRegexList(filterStack));
    }

    @Override
    public GuiSyncMessage onReceiveSettingsMessage(Player player, FilterSettingsMessage message, ItemStack filterStack, ItemStack moduleStack) {
        List<String> l;
        switch (message.op()) {
            case ADD_STRING -> {
                String regex = message.payload().getString("String");
                l = getRegexList(filterStack);
                if (l.size() < MAX_SIZE) {
                    l.add(regex);
                    setRegexList(filterStack, l);
                    return new GuiSyncMessage(filterStack);
                }
            }
            case REMOVE_AT -> {
                int pos = message.payload().getInt("Pos");
                l = getRegexList(filterStack);
                if (pos >= 0 && pos < l.size()) {
                    l.remove(pos);
                    setRegexList(filterStack, l);
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
        return tag != null ? tag.getList(NBT_REGEX, Tag.TAG_STRING).size() : 0;
    }
}
