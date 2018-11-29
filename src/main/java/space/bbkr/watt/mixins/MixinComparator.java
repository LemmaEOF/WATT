package space.bbkr.watt.mixins;

import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.ComparatorMode;
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

@Mixin(BlockRedstoneComparator.class)
public abstract class MixinComparator extends BlockRedstoneDiode implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static EnumProperty<ComparatorMode> MODE;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinComparator(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(HORIZONTAL_FACING, EnumFacing.NORTH).withProperty(POWERED, false).withProperty(MODE, ComparatorMode.COMPARE).withProperty(WATERLOGGED, false));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void InjectComparator(CallbackInfo ci) {
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(HORIZONTAL_FACING, EnumFacing.NORTH).withProperty(POWERED, false).withProperty(MODE, ComparatorMode.COMPARE).withProperty(WATERLOGGED, false));
    }

    @Inject(method = "fillStateContainer", at = @At("TAIL"))
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> state, CallbackInfo ci) {
        state.add(WATERLOGGED);
    }

    @Override
    public IBlockState getStateForPlacement(BlockItemUseContext ctx) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        return this.getDefaultState().withProperty(HORIZONTAL_FACING, ctx.getPlacementHorizontalFacing().getOpposite()).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        if (state.getValue(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleUpdate(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return state;
    }

    public Fluid pickupFluid(IWorld world, BlockPos pos, IBlockState state) {
        if (state.getValue(WATERLOGGED)) {
            world.setBlockState(pos, state.withProperty(WATERLOGGED, false), 3);
            return Fluids.WATER;
        } else {
            return Fluids.EMPTY;
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
