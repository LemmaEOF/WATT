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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    public void InjectLeaves(CallbackInfo ci) {
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(DISTANCE, 7).withProperty(PERSISTENT, false).withProperty(WATERLOGGED, false));
    }

    @Inject(method = "fillStateContainer", at = @At("TAIL"))
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> state, CallbackInfo ci) {
        state.add(WATERLOGGED);
    }

    @Inject(method = "b",
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
        return WattCore.receiveFluidUniversal(world, pos, state, fluid, WATERLOGGED);
    }
}
