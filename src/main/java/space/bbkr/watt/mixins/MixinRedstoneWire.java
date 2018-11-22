package space.bbkr.watt.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.RedstoneSide;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockRedstoneWire.class)
public abstract class MixinRedstoneWire extends Block implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static EnumProperty<RedstoneSide> NORTH;
    @Shadow public static EnumProperty<RedstoneSide> EAST;
    @Shadow public static EnumProperty<RedstoneSide> SOUTH;
    @Shadow public static EnumProperty<RedstoneSide> WEST;
    @Shadow public static IntegerProperty POWER;
    @Shadow protected abstract RedstoneSide getSide(IBlockReader p_getSide_1_, BlockPos p_getSide_2_, EnumFacing p_getSide_3_);

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinRedstoneWire(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(NORTH, RedstoneSide.NONE).withProperty(EAST, RedstoneSide.NONE).withProperty(SOUTH, RedstoneSide.NONE).withProperty(WEST, RedstoneSide.NONE).withProperty(POWER, 0).withProperty(WATERLOGGED, false));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(NORTH, EAST, SOUTH, WEST, POWER, WATERLOGGED);
    }

    public Fluid pickupFluid(IWorld world, BlockPos pos, IBlockState state) {
        if (state.getValue(WATERLOGGED)) {
            world.setBlockState(pos, state.withProperty(WATERLOGGED, false), 3);
            return Fluids.WATER;
        } else {
            return Fluids.EMPTY;
        }
    }

    @Inject(method = "getStateForPlacement",
            at = @At("RETURN"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false)
    public void getWaterloggedState(BlockItemUseContext ctx, CallbackInfoReturnable ci, IBlockReader lvt_2_1_, BlockPos lvt_3_1_) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        IBlockState state = this.getDefaultState().withProperty(WEST, this.getSide(lvt_2_1_, lvt_3_1_, EnumFacing.WEST)).withProperty(EAST, this.getSide(lvt_2_1_, lvt_3_1_, EnumFacing.EAST)).withProperty(NORTH, this.getSide(lvt_2_1_, lvt_3_1_, EnumFacing.NORTH)).withProperty(SOUTH, this.getSide(lvt_2_1_, lvt_3_1_, EnumFacing.SOUTH)).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);

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
