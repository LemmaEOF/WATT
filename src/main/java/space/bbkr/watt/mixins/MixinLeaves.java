package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.bbkr.watt.WattCore;

@Mixin(BlockLeaves.class)
public abstract class MixinLeaves extends Block implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static IntegerProperty DISTANCE;
    @Shadow public static BooleanProperty PERSISTENT;
    @Shadow private static IBlockState updateDistance(IBlockState state, IWorld world, BlockPos pos) {
        return state.withProperty(WATERLOGGED, false);
    }


    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinLeaves(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(DISTANCE, 7).withProperty(PERSISTENT, false).withProperty(WATERLOGGED, false));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(DISTANCE, PERSISTENT, WATERLOGGED);
    }

    @Inject(method = "getStateForPlacement",
            at = @At("RETURN"),
            cancellable = true)
    public void getWaterloggedState(BlockItemUseContext ctx, CallbackInfoReturnable ci) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());
        IBlockState state = updateDistance(this.getDefaultState().withProperty(PERSISTENT, true).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER), ctx.getWorld(), ctx.getPos());

        ci.setReturnValue(state);
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
        if (!state.getValue(WATERLOGGED) && fluid.getFluid() == Fluids.WATER) {
            if (!world.isRemote()) {
                world.setBlockState(pos, state.withProperty(WATERLOGGED, true), 3);
                world.getPendingFluidTicks().scheduleUpdate(pos, fluid.getFluid(), fluid.getFluid().getTickRate(world));
            }

            return true;
        } else {
            return false;
        }
    }
}
