package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
        
    }

    // Mapping-safe: omit @Override and use broader signature
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        
    }

    // 1.21.8 variant used by superclass; keep it to be safe
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) { super.onStateReplaced(state, world, pos, moved); }

    // Mapping-safe variant with Hand parameter
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        // MVP: show inventory in a later pass; for now, only bind on valid inputs

        ItemStack held = player.getStackInHand(hand);
        boolean changed = false;

        if (!held.isEmpty() && held.isOf(Items.RECOVERY_COMPASS)) {
            // Bind compass to this gateway if player has a diamond block
            if (player.getInventory().contains(new ItemStack(Items.DIAMOND_BLOCK))) {
                // consume 1 diamond block
                player.getInventory().remove(item -> item.isOf(Items.DIAMOND_BLOCK), 1, player.getInventory());
                net.minecraft.nbt.NbtCompound tag = new net.minecraft.nbt.NbtCompound();
                tag.putString("bound_gateway", pos.getX()+","+pos.getY()+","+pos.getZ());
                tag.putString("bound_dimension", world.getRegistryKey().getValue().toString());
                held.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.PASS;
    }

    // Mapping-safe override used by 1.21.8 that omits Hand param
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return onUse(state, world, pos, player, Hand.MAIN_HAND, hit);
    }

    @Nullable
    @Override
    public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return null; }
}


