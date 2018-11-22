package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import space.bbkr.watt.WattCore;

@Mixin(BlockLever.class)
public class MixinLever extends BlockHorizontalFace implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static BooleanProperty POWERED;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinLever(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(HORIZONTAL_FACING, EnumFacing.NORTH).withProperty(POWERED, false).withProperty(POWERED, false).withProperty(WATERLOGGED, false).withProperty(FACE, AttachFace.WALL));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(FACE, HORIZONTAL_FACING, POWERED, WATERLOGGED);
    }

    public IBlockState getStateForPlacement(BlockItemUseContext ctx) {
        EnumFacing[] face = ctx.func_196009_e();
        int len = face.length;

        for(int i = 0; i < len; i++) {
            EnumFacing facing = face[i];
            IBlockState state;
            IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());
            if (facing.getAxis() == EnumFacing.Axis.Y) {
                state = this.getDefaultState().withProperty(FACE, facing == EnumFacing.UP ? AttachFace.CEILING : AttachFace.FLOOR).withProperty(HORIZONTAL_FACING, ctx.getPlacementHorizontalFacing()).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
            } else {
                state = this.getDefaultState().withProperty(FACE, AttachFace.WALL).withProperty(HORIZONTAL_FACING, facing.getOpposite()).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
            }

            if (state.isValidPosition(ctx.getWorld(), ctx.getPos())) {
                return state;
            }
        }

        return null;
    }

    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        if (state.getValue(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleUpdate(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return func_196365_i(state).getOpposite() == facing && !state.isValidPosition(world, pos) ? Blocks.AIR.getDefaultState() : super.updatePostPlacement(state, facing, newState, world, pos, posFrom);
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
