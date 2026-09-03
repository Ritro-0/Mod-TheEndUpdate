package com.theendupdate.screen;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

public class GatewayScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final boolean isServer;
    private final net.minecraft.core.BlockPos gatewayPos;
    private final ContainerLevelAccess context;

    public GatewayScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), playerInventory.player.blockPosition());
    }

    public GatewayScreenHandler(int syncId, Inventory playerInventory, Container inventory, net.minecraft.core.BlockPos gatewayPos) {
        super(com.theendupdate.registry.ModScreenHandlers.GATEWAY, syncId);
        this.inventory = inventory;
        this.isServer = !playerInventory.player.level().isClientSide();
        this.gatewayPos = gatewayPos;
        this.context = ContainerLevelAccess.create(playerInventory.player.level(), gatewayPos);

        // input/output slots reuse anvil-style positions: left (27,47), right (76,47), output (134,47)
        this.addSlot(new Slot(inventory, 0, 27, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GatewayScreenHandler.this.isBeaconActiveBelow() && stack.is(Items.RECOVERY_COMPASS);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                GatewayScreenHandler.this.slotsChanged(container);
            }
        });
        this.addSlot(new Slot(inventory, 1, 76, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GatewayScreenHandler.this.isBeaconActiveBelow() && stack.is(Items.DIAMOND_BLOCK);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                GatewayScreenHandler.this.slotsChanged(container);
            }
        });
        this.addSlot(new Slot(inventory, 2, 134, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public boolean mayPickup(Player player) {
                return GatewayScreenHandler.this.isBeaconActiveBelow();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // consume matching count of each input on output pickup
                int taken = stack.getCount();
                if (taken <= 0) taken = 1;
                ItemStack left = container.getItem(0);
                ItemStack right = container.getItem(1);
                if (!left.isEmpty() && left.is(Items.RECOVERY_COMPASS)) {
                    int consume = Math.min(taken, left.getCount());
                    left.shrink(consume);
                    container.setItem(0, left);
                }
                if (!right.isEmpty() && right.is(Items.DIAMOND_BLOCK)) {
                    int consume = Math.min(taken, right.getCount());
                    right.shrink(consume);
                    container.setItem(1, right);
                }
                container.setChanged();
                super.onTake(player, stack);
            }
        });

        int m;
        for (m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }
        this.slotsChanged(inventory);
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!isServer) return;
        for (int i = 0; i < 2; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                this.inventory.setItem(i, ItemStack.EMPTY);
                boolean inserted = player.getInventory().add(stack);
                if (!inserted && !stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
        }
        this.inventory.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int fromIndex) {
        ItemStack empty = ItemStack.EMPTY;
        Slot fromSlot = this.slots.get(fromIndex);
        if (fromSlot == null || !fromSlot.hasItem()) return empty;
        ItemStack fromStack = fromSlot.getItem();
        ItemStack original = fromStack.copy();

        int playerInvStart = 3;
        int playerInvEnd = playerInvStart + 27;
        int hotbarStart = playerInvEnd;
        int hotbarEnd = hotbarStart + 9;

        if (fromIndex == 2) {
            if (!this.isBeaconActiveBelow()) return empty;
            if (!this.moveItemStackTo(fromStack, playerInvStart, hotbarEnd, true)) return empty;
            fromSlot.onQuickCraft(fromStack, original);
            fromSlot.onTake(player, original); // pass pre-transfer stack so inputs are consumed correctly
            return original;
        }

        if (fromIndex >= playerInvStart && fromIndex < hotbarEnd) {
            if (fromStack.is(Items.RECOVERY_COMPASS)) {
                if (!this.moveItemStackTo(fromStack, 0, 1, false)) return empty;
                return original;
            }
            if (fromStack.is(Items.DIAMOND_BLOCK)) {
                if (!this.moveItemStackTo(fromStack, 1, 2, false)) return empty;
                return original;
            }
            return empty;
        }

        if (fromIndex == 0 || fromIndex == 1) {
            if (!this.moveItemStackTo(fromStack, playerInvStart, hotbarEnd, false)) return empty;
            fromSlot.setChanged();
            return original;
        }

        return empty;
    }


    @Override
    public void slotsChanged(Container inv) {
        super.slotsChanged(inv);
        if (!isServer) {
            return;
        }
        if (!this.isBeaconActiveBelow()) {
            inventory.setItem(2, ItemStack.EMPTY);
            inventory.setChanged();
            this.broadcastChanges();
            return;
        }
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (!left.isEmpty() && left.is(Items.RECOVERY_COMPASS)
                && !right.isEmpty() && right.is(Items.DIAMOND_BLOCK)) {
            int maxProduce = Math.min(left.getCount(), right.getCount());
            maxProduce = Math.min(maxProduce, Items.RECOVERY_COMPASS.getDefaultMaxStackSize());
            if (maxProduce <= 0) {
                inventory.setItem(2, ItemStack.EMPTY);
            } else {
                ItemStack out = new ItemStack(Items.RECOVERY_COMPASS, maxProduce);
                out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // visual glint only, no enchantments
                // bind to gateway: store position + dimension in custom data
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putInt("gx", gatewayPos.getX());
                tag.putInt("gy", gatewayPos.getY());
                tag.putInt("gz", gatewayPos.getZ());
                final String[] dimHolder = new String[1];
                this.context.execute((world, pos) -> dimHolder[0] = world.dimension().identifier().toString());
                tag.putString("gd", dimHolder[0] == null ? "" : dimHolder[0]);
                out.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                String name = gatewayPos.getX() + ", " + gatewayPos.getY() + ", " + gatewayPos.getZ();
                out.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                out.set(DataComponents.RARITY, net.minecraft.world.item.Rarity.EPIC); // pink like Heavy Core
                var loreText = net.minecraft.network.chat.Component.literal("Shift+Right-Click to teleport to bound Quantum Gateway.")
                    .withStyle(style -> style.withItalic(true).withColor(net.minecraft.ChatFormatting.GRAY));
                out.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(loreText)));
                inventory.setItem(2, out);
            }
        } else {
            inventory.setItem(2, ItemStack.EMPTY);
        }
        inventory.setChanged();
        this.broadcastChanges();
    }

    private boolean isBeaconActiveBelow() {
        final boolean[] result = new boolean[] { false };
        this.context.execute((world, pos) -> {
            if (!world.getBlockState(pos.below()).is(Blocks.BEACON)) {
                result[0] = false;
                return;
            }
            net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(pos.below());
            if (be instanceof BeaconBlockEntity beacon) {
                try {
                    java.util.List<?> segments = beacon.getBeamSections();
                    result[0] = segments != null && !segments.isEmpty();
                } catch (Throwable ignored) {
                    result[0] = false;
                }
            } else {
                result[0] = false;
            }
        });
        return result[0];
    }
}


