package com.theendupdate.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;

public class GatewayScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final boolean isServer;
    private final net.minecraft.util.math.BlockPos gatewayPos;
    private final ScreenHandlerContext context;

    public GatewayScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), playerInventory.player.getBlockPos());
    }

    public GatewayScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, net.minecraft.util.math.BlockPos gatewayPos) {
        super(com.theendupdate.registry.ModScreenHandlers.GATEWAY, syncId);
        this.inventory = inventory;
        this.isServer = !playerInventory.player.getWorld().isClient;
        this.gatewayPos = gatewayPos;
        this.context = ScreenHandlerContext.create(playerInventory.player.getWorld(), gatewayPos);

        // Inputs at anvil positions (approx): left (27,47), right (76,47); output (134,47)
        this.addSlot(new Slot(inventory, 0, 27, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.RECOVERY_COMPASS);
            }

            @Override
            public void markDirty() {
                super.markDirty();
                GatewayScreenHandler.this.onContentChanged(inventory);
            }
        });
        this.addSlot(new Slot(inventory, 1, 76, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.DIAMOND_BLOCK);
            }

            @Override
            public void markDirty() {
                super.markDirty();
                GatewayScreenHandler.this.onContentChanged(inventory);
            }
        });
        this.addSlot(new Slot(inventory, 2, 134, 47) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                // When the player takes the output, consume matching count of each input
                int taken = stack.getCount();
                if (taken <= 0) taken = 1;
                ItemStack left = inventory.getStack(0);
                ItemStack right = inventory.getStack(1);
                if (!left.isEmpty() && left.isOf(Items.RECOVERY_COMPASS)) {
                    int consume = Math.min(taken, left.getCount());
                    left.decrement(consume);
                    inventory.setStack(0, left);
                }
                if (!right.isEmpty() && right.isOf(Items.DIAMOND_BLOCK)) {
                    int consume = Math.min(taken, right.getCount());
                    right.decrement(consume);
                    inventory.setStack(1, right);
                }
                inventory.markDirty();
                super.onTakeItem(player, stack);
            }
        });

        // Player inventory
        int m;
        for (m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }
        // Initialize output on open
        this.onContentChanged(inventory);
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int fromIndex) {
        ItemStack empty = ItemStack.EMPTY;
        Slot fromSlot = this.slots.get(fromIndex);
        if (fromSlot == null || !fromSlot.hasStack()) return empty;
        ItemStack fromStack = fromSlot.getStack();
        ItemStack original = fromStack.copy();

        int playerInvStart = 3;
        int playerInvEnd = playerInvStart + 27;
        int hotbarStart = playerInvEnd;
        int hotbarEnd = hotbarStart + 9;

        // Output slot → player inventory
        if (fromIndex == 2) {
            if (!this.insertItem(fromStack, playerInvStart, hotbarEnd, true)) return empty;
            fromSlot.onQuickTransfer(fromStack, original);
            // Pass the original stack (pre-transfer count) so inputs are consumed correctly
            fromSlot.onTakeItem(player, original);
            return original;
        }

        // From player inventory/hotbar → inputs
        if (fromIndex >= playerInvStart && fromIndex < hotbarEnd) {
            if (fromStack.isOf(Items.RECOVERY_COMPASS)) {
                if (!this.insertItem(fromStack, 0, 1, false)) return empty;
                return original;
            }
            if (fromStack.isOf(Items.DIAMOND_BLOCK)) {
                if (!this.insertItem(fromStack, 1, 2, false)) return empty;
                return original;
            }
            return empty;
        }

        // From input slots → player inventory
        if (fromIndex == 0 || fromIndex == 1) {
            if (!this.insertItem(fromStack, playerInvStart, hotbarEnd, false)) return empty;
            fromSlot.markDirty();
            return original;
        }

        return empty;
    }

    @Override
    public void onContentChanged(Inventory inv) {
        super.onContentChanged(inv);
        if (!isServer) {
            return;
        }
        ItemStack left = inventory.getStack(0);
        ItemStack right = inventory.getStack(1);
        if (!left.isEmpty() && left.isOf(Items.RECOVERY_COMPASS)
                && !right.isEmpty() && right.isOf(Items.DIAMOND_BLOCK)) {
            int maxProduce = Math.min(left.getCount(), right.getCount());
            maxProduce = Math.min(maxProduce, Items.RECOVERY_COMPASS.getMaxCount());
            if (maxProduce <= 0) {
                inventory.setStack(2, ItemStack.EMPTY);
            } else {
                ItemStack out = new ItemStack(Items.RECOVERY_COMPASS, maxProduce);
                // Visual glint only (no enchantments, no new item)
                out.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                // Bind to gateway: store position and dimension
                net.minecraft.nbt.NbtCompound tag = new net.minecraft.nbt.NbtCompound();
                tag.putInt("gx", gatewayPos.getX());
                tag.putInt("gy", gatewayPos.getY());
                tag.putInt("gz", gatewayPos.getZ());
                // resolve world key via context
                final String[] dimHolder = new String[1];
                this.context.run((world, pos) -> dimHolder[0] = world.getRegistryKey().getValue().toString());
                tag.putString("gd", dimHolder[0] == null ? "" : dimHolder[0]);
                out.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
                // Set default custom name to coordinates
                String name = gatewayPos.getX() + ", " + gatewayPos.getY() + ", " + gatewayPos.getZ();
                out.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
                // Mark as EPIC rarity (pink like Heavy Core)
                out.set(DataComponentTypes.RARITY, net.minecraft.util.Rarity.EPIC);
                inventory.setStack(2, out);
            }
        } else {
            inventory.setStack(2, ItemStack.EMPTY);
        }
        inventory.markDirty();
        this.sendContentUpdates();
    }
}


