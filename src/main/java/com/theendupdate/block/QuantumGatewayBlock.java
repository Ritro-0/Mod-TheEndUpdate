package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.ItemScatterer;
import org.jetbrains.annotations.Nullable;

// no extra imports

public class QuantumGatewayBlock extends BlockWithEntity {
    public QuantumGatewayBlock(Settings settings) {
        super(settings);
    }

    // Desired tint for the beacon beam when passing through this block (C26D84)
    public static final float[] BEAM_TINT = new float[] { 0.7608f, 0.4275f, 0.5176f };

    public static final MapCodec<QuantumGatewayBlock> CODEC = createCodec(QuantumGatewayBlock::new);

    @Override
    public MapCodec<QuantumGatewayBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable net.minecraft.entity.LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        // BlockEntity is constructed by createBlockEntity
    }

    // Mapping-safe: omit @Override and use broader signature
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof QuantumGatewayBlockEntity gateway && !world.isClient) {
                // Drop only the two input slots (0 and 1). Do not drop the output (2).
                for (int i = 0; i < 2; i++) {
                    ItemStack stack = gateway.inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        gateway.inventory.setStack(i, ItemStack.EMPTY);
                    }
                }
                gateway.inventory.markDirty();
                world.updateComparators(pos, this);
            }
        }
        // Intentionally do not call super here; see ServerWorld overload below
    }

    // 1.21.8 variant used by superclass; keep it to be safe
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) { super.onStateReplaced(state, world, pos, moved); }

    // Mapping-safe variant with Hand parameter
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof QuantumGatewayBlockEntity gateway) {
            player.openHandledScreen(gateway);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    // Mapping-safe override used by 1.21.8 that omits Hand param
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return onUse(state, world, pos, player, Hand.MAIN_HAND, hit);
    }

    @Nullable
    @Override
    public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new QuantumGatewayBlockEntity(pos, state); }

    private static void tryCraft(QuantumGatewayBlockEntity gateway) {
        ItemStack compass = gateway.inventory.getStack(0);
        ItemStack diamond = gateway.inventory.getStack(1);
        ItemStack output = gateway.inventory.getStack(2);
        if (!compass.isEmpty() && compass.isOf(Items.RECOVERY_COMPASS)
                && !diamond.isEmpty() && diamond.isOf(Items.DIAMOND_BLOCK)
                && output.isEmpty()) {
            compass.decrement(1);
            diamond.decrement(1);
            gateway.inventory.setStack(2, new ItemStack(Items.RECOVERY_COMPASS));
        }
    }
}


