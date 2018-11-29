package space.bbkr.watt.mixins;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BedPart;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import space.bbkr.watt.WattCore;

@Mixin(BlockBed.class)
public abstract class MixinBed extends BlockHorizontal implements IBucketPickupHandler, ILiquidContainer {

    @Shadow public static EnumProperty<BedPart> PART;
    @Shadow public static BooleanProperty OCCUPIED;

    private static BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MixinBed(Builder builder) {
        super(builder);
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(PART, BedPart.FOOT).withProperty(OCCUPIED, false).withProperty(WATERLOGGED, false));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void InjecBed(CallbackInfo ci) {
        this.setDefaultState(this.stateContainer.getBaseState().withProperty(PART, BedPart.FOOT).withProperty(OCCUPIED, false).withProperty(WATERLOGGED, false));
    }

    @Inject(method = "fillStateContainer", at = @At("TAIL"))
    protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, IBlockState> state, CallbackInfo ci) {
        state.add(WATERLOGGED);
    }

    @Inject(method = "b",
            at = @At("RETURN"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false)
    public void getWaterloggedState(BlockItemUseContext ctx, CallbackInfoReturnable ci, EnumFacing lvt_2_1_, BlockPos lvt_3_1_, BlockPos lvt_4_1_) {
        IFluidState fluid = ctx.getWorld().getFluidState(ctx.getPos());

        IBlockState state = ctx.getWorld().getBlockState(lvt_4_1_).isReplaceable(ctx) ? this.getDefaultState().withProperty(HORIZONTAL_FACING, lvt_2_1_).withProperty(WATERLOGGED, fluid.getFluid() == Fluids.WATER) : null;

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
