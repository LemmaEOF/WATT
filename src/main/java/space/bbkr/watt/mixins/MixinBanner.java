package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.bbkr.watt.WattCore;

@Mixin(BlockBanner.class)
public abstract class MixinBanner extends BlockAbstractBanner implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static IntegerProperty ROTATION;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinBanner(EnumDyeColor color, Builder builder) {
        super(color, builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(ROTATION, 0).withProperty(WATERLOGGED, false));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(ROTATION, WATERLOGGED);
    }

    @Inject(method = "getStateForPlacement",
            at = @At("RETURN"),
            cancellable = true)
    public void getWaterloggedState(BlockItemUseContext ctx, CallbackInfoReturnable ci) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        IBlockState state = this.getDefaultState().withProperty(ROTATION, MathHelper.floor((double)((180.0F + ctx.getPlacementYaw()) * 16.0F / 360.0F) + 0.5D) & 15).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);

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
