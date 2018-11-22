package space.bbkr.watt;

import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.init.Fluids;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.dimdev.riftloader.listener.InitializationListener;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

public class WattCore implements InitializationListener {
    @Override
    public void onInitialization() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.watt.json");
    }

    public static boolean receiveFluidUniversal(IWorld world, BlockPos pos, IBlockState state, IFluidState fluid, BooleanProperty waterlogged) {
        if (!state.getValue(waterlogged) && fluid.getFluid() == Fluids.WATER) {
            if (!world.isRemote()) {
                world.setBlockState(pos, state.withProperty(waterlogged, true), 3);
                world.getPendingFluidTicks().scheduleUpdate(pos, fluid.getFluid(), fluid.getFluid().getTickRate(world));
            }

            return true;
        } else {
            return false;
        }
    }
}
