package me.desht.modularrouters.logic.compiled;

import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.item.module.DetectorModule;
import me.desht.modularrouters.util.ModuleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class CompiledDetectorModule extends CompiledModule {
    public static final String NBT_SIGNAL_LEVEL = "SignalLevel";
    public static final String NBT_STRONG_SIGNAL = "StrongSignal";

    private final int signalLevel;
    private final boolean strongSignal;

    public CompiledDetectorModule(ModularRouterBlockEntity router, ItemStack stack) {
        super(router, stack);

        CompoundTag compound = ModuleHelper.validateNBT(stack);
        signalLevel = compound.contains(NBT_SIGNAL_LEVEL) ? compound.getByte(NBT_SIGNAL_LEVEL) : 15;
        strongSignal = compound.getBoolean(NBT_STRONG_SIGNAL);
    }

    @Override
    public boolean hasTarget() {
        return true;
    }

    @Override
    public boolean execute(@Nonnull ModularRouterBlockEntity router) {
        ItemStack stack = router.getBufferItemStack();

        if (!getFilter().test(stack)) {
            return false;
        }

        router.emitRedstone(getDirection(), getSignalLevel(), DetectorModule.SignalType.getType(isStrongSignal()));

        return true;
    }

    public int getSignalLevel() {
        return signalLevel;
    }

    public boolean isStrongSignal() {
        return strongSignal;
    }

    @Override
    public void onCompiled(ModularRouterBlockEntity router) {
        super.onCompiled(router);
        router.setAllowRedstoneEmission(true);
    }

    @Override
    public void cleanup(ModularRouterBlockEntity router) {
        super.cleanup(router);
        router.setAllowRedstoneEmission(false);
    }
}
