package me.desht.modularrouters.item.module;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.gui.GuiModule;
import me.desht.modularrouters.gui.GuiModulePlayer;
import me.desht.modularrouters.logic.CompiledModule;
import me.desht.modularrouters.logic.CompiledPlayerModule;
import me.desht.modularrouters.util.InventoryUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class PlayerModule extends Module {
    @Override
    public boolean execute(TileEntityItemRouter router, CompiledModule compiled) {
        CompiledPlayerModule cpm = (CompiledPlayerModule) compiled;
        EntityPlayer player = cpm.getPlayer();
        if (player == null) {
            return false;
        }
        IItemHandler itemHandler = getHandler(player, cpm);
        if (itemHandler == null) {
            return false;
        }
        ItemStack bufferStack = router.getBufferItemStack();
        switch (cpm.getOperation()) {
            case EXTRACT:
                if (bufferStack == null || bufferStack.stackSize < bufferStack.getMaxStackSize()) {
                    int taken = InventoryUtils.extractItems(itemHandler, compiled, router);
                    return taken > 0;
                }
                break;
            case INSERT:
                if (bufferStack != null && compiled.getFilter().pass(bufferStack)) {
                    if (cpm.getSection() == CompiledPlayerModule.Section.ARMOR) {
                        return insertArmor(router, player, itemHandler, bufferStack);
                    } else {
                        int sent = InventoryUtils.transferItems(router.getBuffer(), itemHandler, 0, router.getItemsPerTick());
                        return sent > 0;
                    }
                }
                break;
            default: return false;
        }
        return false;
    }

    private boolean insertArmor(TileEntityItemRouter router, EntityPlayer player, IItemHandler itemHandler, ItemStack armorStack) {
        int slot = getSlotForArmorItem(player, armorStack);
        if (slot < 0) {
            return false;  // not an armor item
        }
        if (itemHandler.getStackInSlot(slot) != null) {
            return false;  // already armor in this slot
        }
        ItemStack extracted = router.getBuffer().extractItem(0, 1, false);
        if (extracted == null) {
            return false;
        }
        ItemStack res = itemHandler.insertItem(slot, extracted, false);
        return res == null;
    }

    private int getSlotForArmorItem(EntityPlayer player, ItemStack stack) {
        EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
        switch (slot) {
            case HEAD: return 3;
            case CHEST: return 2;
            case LEGS: return 1;
            case FEET: return 0;
            default: return -1;
        }
    }

    private IItemHandler getHandler(EntityPlayer player, CompiledPlayerModule cpm) {
        switch (cpm.getSection()) {
            case MAIN: return new ItemStackHandler(player.inventory.mainInventory);
            case ARMOR: return new ItemStackHandler(player.inventory.armorInventory);
            case OFFHAND: return new ItemStackHandler(player.inventory.offHandInventory);
            default: return null;
        }
    }


    @Override
    public CompiledModule compile(TileEntityItemRouter router, ItemStack stack) {
        return new CompiledPlayerModule(router, stack);
    }

    @Override
    protected void addExtraInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean par4) {
        super.addExtraInformation(stack, player, list, par4);
        CompiledPlayerModule cpm = new CompiledPlayerModule(null, stack);
        list.add(TextFormatting.YELLOW + I18n.format("itemText.security.owner", cpm.getPlayerName()));
        list.add(TextFormatting.YELLOW + String.format(TextFormatting.YELLOW + "%s: " + TextFormatting.AQUA + "%s %s %s",
                I18n.format("itemText.misc.operation"),
                I18n.format("guiText.label.playerOp." + cpm.getOperation()),
                cpm.getOperation().getSymbol(),
                I18n.format("guiText.label.playerSect." + cpm.getSection())));
    }

    @Override
    public Class<? extends GuiModule> getGuiHandler() {
        return GuiModulePlayer.class;
    }

    @Override
    public boolean isDirectional() {
        return false;
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing face, float x, float y, float z) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking()) {
            ItemModule.setOwner(stack, player);
            player.addChatMessage(new TextComponentTranslation("itemText.security.owner", player.getDisplayNameString()));
            return EnumActionResult.SUCCESS;
        } else {
            return super.onItemUse(stack, player, world, pos, hand, face, x, y, z);
        }
    }
}
