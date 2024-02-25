package me.desht.modularrouters.logic.compiled;

import com.mojang.authlib.GameProfile;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.util.TranslatableEnum;
import me.desht.modularrouters.item.module.PlayerModule;
import me.desht.modularrouters.util.InventoryUtils;
import me.desht.modularrouters.util.WildcardedRLMatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class CompiledPlayerModule extends CompiledModule {
    public static final String NBT_OPERATION = "Operation";
    public static final String NBT_SECTION = "Section";

    public enum Operation implements TranslatableEnum {
        EXTRACT, INSERT;

        public String getSymbol() { return this == INSERT ? "⟹" : "⟸"; }

        @Override
        public String getTranslationKey() {
            return "modularrouters.guiText.label.playerOp." + this;
        }
    }

    public enum Section implements TranslatableEnum {
        MAIN, MAIN_NO_HOTBAR, ARMOR, OFFHAND, ENDER;

        @Override
        public String getTranslationKey() {
            return "modularrouters.guiText.label.playerSect." + this;
        }
    }

    private final Operation operation;
    private final Section section;
    private final GameProfile playerProfile;
    private WeakReference<Player> playerRef;

    public CompiledPlayerModule(ModularRouterBlockEntity router, ItemStack stack) {
        super(router, stack);

        CompoundTag compound = stack.getTagElement(ModularRouters.MODID);
        if (compound != null) {
            playerProfile = ((PlayerModule) stack.getItem()).getOwnerProfile(stack).orElse(null);
            operation = Operation.values()[compound.getInt(NBT_OPERATION)];
            section = Section.values()[compound.getInt(NBT_SECTION)];
            if (router != null && !router.nonNullLevel().isClientSide) {
                Player player = playerProfile == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerProfile.getId());
                playerRef = new WeakReference<>(player);
            } else {
                playerRef = new WeakReference<>(null);
            }
        } else {
            operation = Operation.EXTRACT;
            section = Section.MAIN;
            playerProfile = null;
        }
    }

    @Override
    public boolean hasTarget() {
        return getPlayer() != null;
    }

    @Override
    public boolean execute(@Nonnull ModularRouterBlockEntity router) {
        Player player = getPlayer();  // will be non-null if we get here

        if (isDimensionBlacklisted(router, player)) {
            return false;
        }

        IItemHandler itemHandler = getHandler(player);
        if (itemHandler == null) {
            return false;
        }

        ItemStack bufferStack = router.getBufferItemStack();
        switch (operation) {
            case EXTRACT -> {
                if (bufferStack.getCount() < bufferStack.getMaxStackSize()) {
                    ItemStack taken = transferToRouter(itemHandler, null, router);
                    return !taken.isEmpty();
                }
            }
            case INSERT -> {
                if (getFilter().test(bufferStack)) {
                    if (getSection() == Section.ARMOR) {
                        return insertArmor(router, itemHandler, bufferStack);
                    } else {
                        int nToSend = getItemsPerTick(router);
                        if (getRegulationAmount() > 0) {
                            int existing = InventoryUtils.countItems(bufferStack, itemHandler, getRegulationAmount(), !getFilter().getFlags().isIgnoreDamage());
                            nToSend = Math.min(nToSend, getRegulationAmount() - existing);
                            if (nToSend <= 0) {
                                return false;
                            }
                        }
                        int sent = InventoryUtils.transferItems(router.getBuffer(), itemHandler, 0, nToSend);
                        return sent > 0;
                    }
                }
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private boolean isDimensionBlacklisted(ModularRouterBlockEntity router, Player player) {
        WildcardedRLMatcher matcher = ModularRouters.getDimensionBlacklist();
        return matcher.test(router.nonNullLevel().dimension().location()) || matcher.test(player.level().dimension().location());
    }

    private Player getPlayer() {
        return playerRef == null ? null : playerRef.get();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getUUID().equals(getPlayerId())) {
            playerRef = new WeakReference<>(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getUUID().equals(getPlayerId())) {
            playerRef = new WeakReference<>(null);
        }
    }

    @Override
    public void onCompiled(ModularRouterBlockEntity router) {
        super.onCompiled(router);
        if (!router.nonNullLevel().isClientSide) {
            NeoForge.EVENT_BUS.register(this);
        }
    }

    @Override
    public void cleanup(ModularRouterBlockEntity router) {
        super.cleanup(router);
        if (!router.nonNullLevel().isClientSide) {
            NeoForge.EVENT_BUS.unregister(this);
        }
    }

    public UUID getPlayerId() {
        return playerProfile == null ? null : playerProfile.getId();
    }

    public String getPlayerName() {
        return playerProfile == null ? null : playerProfile.getName();
    }

    public Operation getOperation() {
        return operation;
    }

    public Section getSection() {
        return section;
    }

    private boolean insertArmor(ModularRouterBlockEntity router, IItemHandler itemHandler, ItemStack armorStack) {
        int slot = getSlotForArmorItem(armorStack);
        if (slot >= 0 && itemHandler.getStackInSlot(slot).isEmpty()) {
            ItemStack extracted = router.getBuffer().extractItem(0, 1, false);
            if (extracted.isEmpty()) {
                return false;
            }
            ItemStack res = itemHandler.insertItem(slot, extracted, false);
            return res.isEmpty();
        } else {
            return false;
        }
    }

    private int getSlotForArmorItem(ItemStack stack) {
        return switch (Mob.getEquipmentSlotForItem(stack)) {
            case HEAD -> 3;
            case CHEST -> 2;
            case LEGS -> 1;
            case FEET -> 0;
            default -> -1;
        };
    }

    private IItemHandler getHandler(Player player) {
        return switch (section) {
            case MAIN -> new PlayerMainInvWrapper(player.getInventory());
            case MAIN_NO_HOTBAR -> new PlayerMainInvNoHotbarWrapper(player.getInventory());
            case ARMOR -> new PlayerArmorInvWrapper(player.getInventory());
            case OFFHAND -> new PlayerOffhandInvWrapper(player.getInventory());
            case ENDER -> new InvWrapper(player.getEnderChestInventory());
        };
    }

    public static class PlayerMainInvNoHotbarWrapper extends RangedWrapper {
        PlayerMainInvNoHotbarWrapper(Inventory inv) {
            super(new InvWrapper(inv), Inventory.getSelectionSize(), inv.items.size());
        }
    }
}
