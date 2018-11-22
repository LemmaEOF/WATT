package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoorHingeSide;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import space.bbkr.watt.WattCore;

import javax.annotation.Nullable;

@Mixin(BlockDoor.class)
public abstract class MixinDoor extends Block implements IBucketPickupHandler, ILiquidContainer {
    @Shadow public static DirectionProperty FACING;
    @Shadow public static BooleanProperty OPEN;
    @Shadow public static EnumProperty<DoorHingeSide> HINGE;
    @Shadow public static BooleanProperty POWERED;
    @Shadow public static EnumProperty<DoubleBlockHalf> HALF;
    @Shadow protected abstract DoorHingeSide func_208073_b(BlockItemUseContext ctx);

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinDoor(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(OPEN, false).withProperty(HINGE, DoorHingeSide.LEFT).withProperty(POWERED, false).withProperty(HALF, DoubleBlockHalf.LOWER).withProperty(WATERLOGGED, false));
    }

    /**
     * @author b0undarybreaker
     * @reason need to add waterlogged property
     */
    @Overwrite
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> p_fillStateContainer_1_) {
        p_fillStateContainer_1_.add(HALF, FACING, OPEN, HINGE, POWERED, WATERLOGGED);
    }

    /**
     * @author b0undarybreaker
     * @reason something is wrong with the local variable table so I had to rip it wholecloth and rewrite
     */
    @Overwrite
    @Nullable
    public IBlockState getStateForPlacement(BlockItemUseContext ctx) {
        BlockPos pos = ctx.getPos();
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());
        if (pos.getY() < 255 && ctx.getWorld().getBlockState(pos.up()).isReplaceable(ctx)) {
            World world = ctx.getWorld();
            boolean isOpen = world.isBlockPowered(pos) || world.isBlockPowered(pos.up());
            return this.getDefaultState().withProperty(FACING, ctx.getPlacementHorizontalFacing()).withProperty(HINGE, this.func_208073_b(ctx)).withProperty(POWERED, isOpen).withProperty(OPEN, isOpen).withProperty(HALF, DoubleBlockHalf.LOWER).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
        } else {
            return null;
        }
    }

    /**
     * @author b0undarybreaker
     * @reason make it so door halves can waterlog independently
     */
    @Overwrite
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        IFluidState fluid = world.getFluidState(pos.up());
        world.setBlockState(pos.up(), state.withProperty(HALF, DoubleBlockHalf.UPPER).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER), 3);
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
