package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.RailShape;
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
import space.bbkr.watt.WattCore;

@Mixin(BlockRail.class)
public abstract class MixinRails extends BlockRailBase implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static EnumProperty<RailShape> SHAPE;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinRails(Properties builder) {
        super(false, builder);
        this.setDefaultState(this.stateContainer.getBaseState().with(SHAPE, RailShape.NORTH_SOUTH).with(WATERLOGGED, false));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void InjectRails(CallbackInfo ci) {
        this.setDefaultState(this.stateContainer.getBaseState().with(SHAPE, RailShape.NORTH_SOUTH).with(WATERLOGGED, false));
    }

    @Inject(method = "fillStateContainer", at = @At("TAIL"))
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> state, CallbackInfo ci) {
        state.add(WATERLOGGED);
    }

    @Override
    public IBlockState getStateForPlacement(BlockItemUseContext ctx) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        return this.getDefaultState().with(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        if (state.get(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return state;
    }

    public Fluid pickupFluid(IWorld world, BlockPos pos, IBlockState state) {
        if (state.get(WATERLOGGED)) {
            world.setBlockState(pos, state.with(WATERLOGGED, false), 3);
            return Fluids.WATER;
        } else {
            return Fluids.EMPTY;
        }
    }

    public IFluidState getFluidState(IBlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    public boolean canContainFluid(IBlockReader reader, BlockPos pos, IBlockState state, Fluid fluid) {
        return !state.get(WATERLOGGED) && fluid == Fluids.WATER;
    }

    public boolean receiveFluid(IWorld world, BlockPos pos, IBlockState state, IFluidState fluid) {
        return WattCore.receiveFluidUniversal(world, pos, state, fluid, WATERLOGGED);
    }
}
