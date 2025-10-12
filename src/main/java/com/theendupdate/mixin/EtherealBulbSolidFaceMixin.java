package com.theendupdate.mixin;

import com.theendupdate.block.EtherealBulbButtonBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class EtherealBulbSolidFaceMixin {
    
    @Inject(method = "isSideSolidFullSquare", at = @At("HEAD"), cancellable = true)
    private void makeEtherealBulbSolidOnAttachmentFace(BlockView world, BlockPos pos, Direction side, 
                                                       CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        
        // Check if this is an ethereal bulb button
        if (state.getBlock() instanceof EtherealBulbButtonBlock) {
            try {
                // Get the face the button is attached to using ButtonBlock's properties
                BlockFace face = state.get(ButtonBlock.FACE);
                
                // For floor-mounted (on top of walls/fences), report bottom as solid
                if (face == BlockFace.FLOOR && side == Direction.DOWN) {
                    cir.setReturnValue(true);
                    return;
                }
                
                // For ceiling-mounted, report top as solid
                if (face == BlockFace.CEILING && side == Direction.UP) {
                    cir.setReturnValue(true);
                    return;
                }
                
                // For wall-mounted, report the attachment side as solid
                if (face == BlockFace.WALL) {
                    Direction facing = state.get(ButtonBlock.FACING);
                    if (side == facing.getOpposite()) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // If property access fails, fall through to default behavior
            }
        }
    }
}

