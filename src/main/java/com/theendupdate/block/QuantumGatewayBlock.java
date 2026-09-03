package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class QuantumGatewayBlock extends BaseEntityBlock {
    public QuantumGatewayBlock(Properties settings) {
        super(settings);
    }

    // beam tint for this block, hex C26D84
    public static final float[] BEAM_TINT = new float[] { 0.7608f, 0.4275f, 0.5176f };

    public static final MapCodec<QuantumGatewayBlock> CODEC = simpleCodec(QuantumGatewayBlock::new);

    // Matches the Blockbench model: 13px body, roof sparkle to 14px, corner pillars to 20px.
    private static final VoxelShape SHAPE = Shapes.or(
        Shapes.box(0.0, 0.0, 0.0, 1.0, 13.0 / 16.0, 1.0),
        Shapes.box(6.0 / 16.0, 13.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0, 14.0 / 16.0, 10.0 / 16.0),
        Shapes.box(3.0 / 16.0, 13.0 / 16.0, 3.0 / 16.0, 5.0 / 16.0, 20.0 / 16.0, 5.0 / 16.0),
        Shapes.box(3.0 / 16.0, 13.0 / 16.0, 11.0 / 16.0, 5.0 / 16.0, 20.0 / 16.0, 13.0 / 16.0),
        Shapes.box(11.0 / 16.0, 13.0 / 16.0, 3.0 / 16.0, 13.0 / 16.0, 20.0 / 16.0, 5.0 / 16.0),
        Shapes.box(11.0 / 16.0, 13.0 / 16.0, 11.0 / 16.0, 13.0 / 16.0, 20.0 / 16.0, 13.0 / 16.0)
    );

    @Override
    public MapCodec<QuantumGatewayBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        // immovable - pushing this would lose the inventory contents
        return PushReaction.BLOCK;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        // BE already constructed by newBlockEntity, nothing else needed here
    }

    // Mapping-safe: omit @Override and use broader signature
    public void onStateReplaced(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof QuantumGatewayBlockEntity gateway && !world.isClientSide()) {
                // only drop input slots 0/1, not the output slot 2
                for (int i = 0; i < 2; i++) {
                    ItemStack stack = gateway.inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        gateway.inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
                gateway.inventory.setChanged();
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        // Intentionally do not call super here; see ServerWorld overload below
    }

    // 1.21.8 variant used by superclass; keep it to be safe
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) { super.affectNeighborsAfterRemoval(state, world, pos, moved); }

    // Mapping-safe variant with Hand parameter
    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof QuantumGatewayBlockEntity gateway) {
            player.openMenu(gateway);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // Mapping-safe override used by 1.21.8 that omits Hand param
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return onUse(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new QuantumGatewayBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (type != com.theendupdate.registry.ModBlockEntities.QUANTUM_GATEWAY) {
            return null;
        }
        return (w, p, s, be) -> QuantumGatewayBlockEntity.tick(w, p, s, (QuantumGatewayBlockEntity) be);
    }
}


