package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReaderBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import space.bbkr.watt.WattCore;

@Mixin(BlockBannerWall.class)
public abstract class MixinBannerWall extends BlockAbstractBanner implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static DirectionProperty HORIZONTAL_FACING;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinBannerWall(EnumDyeColor color, Builder builder) {
        super(color, builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(HORIZONTAL_FACING, EnumFacing.NORTH).withProperty(WATERLOGGED, false));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(HORIZONTAL_FACING, WATERLOGGED);
    }

    @Inject(method = "getStateForPlacement",
            at = @At("RETURN"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false)
    public void getWaterloggedState(BlockItemUseContext ctx, CallbackInfoReturnable ci, IBlockState lvt_2_1_, IWorldReaderBase lvt_3_1_, BlockPos lvt_4_1_) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        if (lvt_2_1_.isValidPosition(lvt_3_1_, lvt_4_1_)) {
            ci.setReturnValue(lvt_2_1_.withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER));
        } else ci.setReturnValue(null);
        ci.cancel();
    }

    @Inject(method = "updatePostPlacement",
            at = @At("HEAD"))
    public void updateWaterloggedState(IBlockState state, EnumFacing facing, IBlockState newState, IWorld world, BlockPos pos, BlockPos posFrom, CallbackInfoReturnable ci) {
        if (state.getValue(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleUpdate(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
    }

    public IFluidState getFluidState(IBlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    public boolean canContainFluid(IBlockReader reader, BlockPos pos, IBlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }

    public boolean receiveFluid(IWorld world, BlockPos pos, IBlockState state, IFluidState fluid) {
        return WattCore.receiveFluidUniversal(world, pos, state, fluid, WATERLOGGED);
    }
}
