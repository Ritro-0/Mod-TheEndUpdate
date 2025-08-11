package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;
public class VoidBloomBlock extends net.minecraft.block.PlantBlock {
    public static final MapCodec<VoidBloomBlock> CODEC = createCodec(VoidBloomBlock::new);
    // Track which direction the void bloom is attached to (the direction TO the chorus bud)
    public static final Property<Direction> ATTACHMENT_FACE = Properties.FACING;

    public VoidBloomBlock(Settings settings) {
        super(settings);
        // Default attachment is downward (sitting on top of something)
        this.setDefaultState(this.stateManager.getDefaultState().with(ATTACHMENT_FACE, Direction.DOWN));
    }

    @Override
    public MapCodec<? extends net.minecraft.block.PlantBlock> getCodec() { 
        return CODEC; 
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ATTACHMENT_FACE);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, net.minecraft.world.BlockView world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction attachmentFace = state.get(ATTACHMENT_FACE);
        
        // Check if there's a chorus flower in the attachment direction
        BlockPos attachedPos = pos.offset(attachmentFace);
        BlockState attachedState = world.getBlockState(attachedPos);
        
        if (attachedState.isOf(Blocks.CHORUS_FLOWER)) {
            return true;
        }
        
        // Check if the attached block has a solid face for the void bloom to attach to
        // The void bloom attaches TO the attachmentFace direction, so we check the opposite face
        Direction oppositeFace = attachmentFace.getOpposite();
        boolean canPlace = attachedState.isSideSolidFullSquare(world, attachedPos, oppositeFace);
        
        return canPlace;
    }

    /**
     * Override to handle proper facing when placing the block
     */
    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        // Get the face that was clicked
        Direction clickedFace = context.getSide();
        
        // If placing against a chorus flower, attach to that face
        BlockPos clickedPos = context.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = clickedPos.offset(direction);
            if (context.getWorld().getBlockState(adjacentPos).isOf(Blocks.CHORUS_FLOWER)) {
                // Attach to the chorus flower face
                return this.getAttachedState(direction.getOpposite());
            }
        }
        
        // For any other block, attach to the clicked face
        // The attachment face is the direction FROM the void bloom TO the clicked surface
        // So if we clicked the UP face, the void bloom should attach downward (opposite direction)
        Direction attachmentDirection = clickedFace.getOpposite();
        return this.getAttachedState(attachmentDirection);
    }

    /**
     * Helper method to create a void bloom state attached to a specific chorus flower
     */
    public BlockState getAttachedState(Direction chorusDirection) {
        // The attachment face is the direction FROM the void bloom TO the chorus flower
        return this.getDefaultState().with(ATTACHMENT_FACE, chorusDirection);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if still supported, break if not
        if (!this.canPlaceAt(state, world, pos)) {
            world.breakBlock(pos, true);
        }
    }
}


