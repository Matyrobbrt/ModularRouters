package me.desht.modularrouters.util;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.logic.CompiledModule;
import me.desht.modularrouters.logic.Filter;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.VanillaDoubleChestItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.util.Random;

public class InventoryUtils {
    public static void dropInventoryItems(World world, BlockPos pos, IItemHandler itemHandler) {
        Random random = new Random();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack itemStack = itemHandler.getStackInSlot(i);
            if (itemStack != null) {
                float offsetX = random.nextFloat() * 0.8f + 0.1f;
                float offsetY = random.nextFloat() * 0.8f + 0.1f;
                float offsetZ = random.nextFloat() * 0.8f + 0.1f;
                while (itemStack.stackSize > 0) {
                    int stackSize = random.nextInt(21) + 10;
                    if (stackSize > itemStack.stackSize) {
                        stackSize = itemStack.stackSize;
                    }

                    itemStack.stackSize -= stackSize;
                    EntityItem entityitem = new EntityItem(world, pos.getX() + (double) offsetX, pos.getY() + (double) offsetY, pos.getZ() + (double) offsetZ, new ItemStack(itemStack.getItem(), stackSize, itemStack.getMetadata()));
                    if (itemStack.hasTagCompound()) {
                        entityitem.getEntityItem().setTagCompound(itemStack.getTagCompound().copy());
                    }

                    float motionScale = 0.05f;
                    entityitem.motionX = random.nextGaussian() * (double) motionScale;
                    entityitem.motionY = random.nextGaussian() * (double) motionScale + 0.20000000298023224D;
                    entityitem.motionZ = random.nextGaussian() * (double) motionScale;
                    world.spawnEntityInWorld(entityitem);
                }
            }
        }
    }

    // Adapted from Botania's InventoryHelper class (which was in turned adapted from OpenBlocks...)
    public static IItemHandler getInventory(World world, BlockPos pos, EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return null;
        }

        if (te instanceof TileEntityChest) {
            IItemHandler doubleChest = VanillaDoubleChestItemHandler.get(((TileEntityChest) te));
            if (doubleChest != VanillaDoubleChestItemHandler.NO_ADJACENT_CHESTS_INSTANCE)
                return doubleChest;
        }

        IItemHandler ret = te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) ?
                te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) : null;

        if (ret == null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
            ret = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // backwards compatibility with mods which aren't using capabilities yet
        if (ret == null) {
            if (side != null && te instanceof ISidedInventory) {
                ret = new SidedInvWrapper((ISidedInventory) te, side);
            } else if (te instanceof IInventory) {
                ret = new InvWrapper((IInventory) te);
            }
        }

        return ret;
    }

    public static int transferItems(IItemHandler from, IItemHandler to, int slot, int count) {
        if (from == null || to == null || count == 0) {
            return 0;
        }
        ItemStack toSend = from.extractItem(slot, count, true);
        if (toSend == null) {
            return 0;
        }
        ItemStack excess = ItemHandlerHelper.insertItem(to, toSend, false);
        int inserted = toSend.stackSize - (excess == null ? 0 : excess.stackSize);
        from.extractItem(slot, inserted, false);
        return inserted;
    }

    public static int extractItems(IItemHandler handler, CompiledModule compiled, TileEntityItemRouter router) {
        ItemStack bufferStack = router.getBufferItemStack();
        Filter filter = compiled.getFilter();
        for (int i = 0; i < handler.getSlots(); i++) {
            int pos = compiled.getLastMatchPos(i, handler.getSlots());
            ItemStack toExtract = handler.extractItem(pos, router.getItemsPerTick(), true);
            if (toExtract != null && filter.pass(toExtract)
                    && (bufferStack == null || ItemHandlerHelper.canItemStacksStack(bufferStack, toExtract))) {
                ItemStack excess = router.getBuffer().insertItem(0, toExtract, false);
                int taken = toExtract.stackSize - (excess == null ? 0 : excess.stackSize);
                handler.extractItem(pos, taken, false);
                compiled.setLastMatchPos(pos);
                return taken;
            }
        }
        return 0;
    }
}
