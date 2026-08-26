package com.blockvehicle.block;

import com.blockvehicle.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PassengerSeatBlock
extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public PassengerSeatBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((this.getStateManager().getDefaultState()).with(FACING, Direction.NORTH));
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (player.getMainHandStack().isOf(ModItems.VEHICLE_WAND) || player.getOffHandStack().isOf(ModItems.VEHICLE_WAND)) {
            return ActionResult.PASS;
        }
        player.sendMessage((Text)Text.literal("\u00a7eActivate the \u00a7bVehicle Core \u00a7efirst to ride!"), true);
        return ActionResult.SUCCESS;
    }
}

