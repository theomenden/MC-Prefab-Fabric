package com.wuest.prefab.structures.predefined;

import com.wuest.prefab.Prefab;
import com.wuest.prefab.Tuple;
import com.wuest.prefab.blocks.FullDyeColor;
import com.wuest.prefab.structures.base.BuildBlock;
import com.wuest.prefab.structures.base.BuildingMethods;
import com.wuest.prefab.structures.base.Structure;
import com.wuest.prefab.structures.config.BasicStructureConfiguration;
import com.wuest.prefab.structures.config.BasicStructureConfiguration.EnumBasicStructureName;
import com.wuest.prefab.structures.config.StructureConfiguration;
import com.wuest.prefab.structures.config.enums.FarmAdvancedOptions;
import com.wuest.prefab.structures.config.enums.BaseOption;
import com.wuest.prefab.structures.config.enums.FarmImprovedOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;

/**
 * This is the basic structure to be used for structures which don't need a lot of configuration or a custom player
 * created structures.
 *
 * @author WuestMan
 */
public class StructureBasic extends Structure {
    private final ArrayList<BlockPos> mobSpawnerPos = new ArrayList<>();
    private BlockPos customBlockPos = null;

    @Override
    protected Boolean CustomBlockProcessingHandled(StructureConfiguration configuration, BuildBlock block, Level world, BlockPos originalPos,
                                                   Block foundBlock, BlockState blockState, Player player) {
        BasicStructureConfiguration config = (BasicStructureConfiguration) configuration;

        String structureName = config.basicStructureName.getName();
        BaseOption chosenOption = config.chosenOption;

        if (foundBlock instanceof HopperBlock && structureName.equals(EnumBasicStructureName.FarmImproved.getName()) && chosenOption == FarmImprovedOptions.AutomatedChickenCoop) {
            this.customBlockPos = block.getStartingPosition().getRelativePosition(
                    originalPos,
                    this.getClearSpace().getShape().getDirection(),
                    configuration.houseFacing);
        } else if (foundBlock instanceof TrapDoorBlock && structureName.equals(EnumBasicStructureName.MineshaftEntrance.getName())) {
            this.customBlockPos = block.getStartingPosition().getRelativePosition(
                    originalPos,
                    this.getClearSpace().getShape().getDirection(),
                    configuration.houseFacing);
        } else if (foundBlock instanceof BedBlock && config.chosenOption.getHasBedColor()) {
            // Even if a structure has a bed; we may want to keep a specific color to match what the design of the structure is.
            BlockPos bedHeadPosition = block.getStartingPosition().getRelativePosition(originalPos, this.getClearSpace().getShape().getDirection(), configuration.houseFacing);
            BlockPos bedFootPosition = block.getSubBlock().getStartingPosition().getRelativePosition(
                    originalPos,
                    this.getClearSpace().getShape().getDirection(),
                    configuration.houseFacing);

            Tuple<BlockState, BlockState> blockStateTuple = BuildingMethods.getBedState(bedHeadPosition, bedFootPosition, config.bedColor);
            block.setBlockState(blockStateTuple.getFirst());
            block.getSubBlock().setBlockState(blockStateTuple.getSecond());

            this.priorityOneBlocks.add(block);
            return true;
        } else if (foundBlock instanceof SpawnerBlock && structureName.equals(EnumBasicStructureName.FarmAdvanced.getName()) && chosenOption == FarmAdvancedOptions.MonsterMasher
                && Prefab.serverConfiguration.includeSpawnersInMasher) {
            this.mobSpawnerPos.add(block.getStartingPosition().getRelativePosition(
                    originalPos,
                    this.getClearSpace().getShape().getDirection(),
                    configuration.houseFacing));
        }

        return false;
    }

    @Override
    protected Boolean BlockShouldBeClearedDuringConstruction(StructureConfiguration configuration, Level world, BlockPos originalPos, BlockPos blockPos) {
        BasicStructureConfiguration config = (BasicStructureConfiguration) configuration;

        if (config.basicStructureName.getName().equals(EnumBasicStructureName.AquaBase.getName())
                || config.basicStructureName.getName().equals(EnumBasicStructureName.AquaBaseImproved.getName())) {
            BlockState blockState = world.getBlockState(blockPos);
            // Don't clear water blocks for this building.
            return blockState.getMaterial() != Material.WATER;
        }

        return true;
    }

    /**
     * This method is used after the main building is build for any additional structures or modifications.
     *
     * @param configuration The structure configuration.
     * @param world         The current world.
     * @param originalPos   The original position clicked on.
     * @param player        The player which initiated the construction.
     */
    @Override
    public void AfterBuilding(StructureConfiguration configuration, ServerLevel world, BlockPos originalPos, Player player) {
        BasicStructureConfiguration config = (BasicStructureConfiguration) configuration;
        String structureName = config.basicStructureName.getName();
        BaseOption chosenOption = config.chosenOption;

        if (this.customBlockPos != null) {
            if (structureName.equals(EnumBasicStructureName.FarmImproved.getName()) && chosenOption == FarmImprovedOptions.AutomatedChickenCoop) {
                // For the advanced chicken coop, spawn 4 chickens above the hopper.
                for (int i = 0; i < 4; i++) {
                    Chicken entity = new Chicken(EntityType.CHICKEN, world);
                    entity.setPos(this.customBlockPos.getX(), this.customBlockPos.above().getY(), this.customBlockPos.getZ());
                    world.addFreshEntity(entity);
                }
            }

            this.customBlockPos = null;
        }

        if (structureName.equals(EnumBasicStructureName.FarmAdvanced.getName()) && chosenOption == FarmAdvancedOptions.MonsterMasher) {
            int monstersPlaced = 0;

            // Set the spawner.
            for (BlockPos pos : this.mobSpawnerPos) {
                BlockEntity tileEntity = world.getBlockEntity(pos);

                if (tileEntity instanceof SpawnerBlockEntity) {
                    SpawnerBlockEntity spawner = (SpawnerBlockEntity) tileEntity;

                    switch (monstersPlaced) {
                        case 0: {
                            // Zombie.
                            spawner.getSpawner().setEntityId(EntityType.ZOMBIE);
                            break;
                        }

                        case 1: {
                            // Skeleton.
                            spawner.getSpawner().setEntityId(EntityType.SKELETON);
                            break;
                        }

                        case 2: {
                            // Cave Spider.
                            spawner.getSpawner().setEntityId(EntityType.CAVE_SPIDER);
                            break;
                        }

                        default: {
                            // Creeper.
                            spawner.getSpawner().setEntityId(EntityType.CREEPER);
                            break;
                        }
                    }

                    monstersPlaced++;
                }
            }
        }

        if (structureName.equals(EnumBasicStructureName.AquaBase.getName())
                || structureName.equals(EnumBasicStructureName.AquaBaseImproved.getName())) {
            // Replace the entrance area with air blocks.
            BlockPos airPos = originalPos.above(4).relative(configuration.houseFacing.getOpposite(), 1);

            // This is the first wall.
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            airPos = airPos.below();
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            airPos = airPos.below();
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            airPos = airPos.below();
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            // Second part of the wall.
            airPos = airPos.relative(configuration.houseFacing.getOpposite()).above();
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            airPos = airPos.above();
            world.removeBlock(airPos.relative(configuration.houseFacing.getClockWise()), false);
            world.removeBlock(airPos, false);
            world.removeBlock(airPos.relative(configuration.houseFacing.getCounterClockWise()), false);

            airPos = airPos.above();
            world.removeBlock(airPos, false);
        }
    }

    @Override
    protected boolean hasGlassColor(StructureConfiguration configuration) {
        BasicStructureConfiguration config = (BasicStructureConfiguration) configuration;
        BaseOption chosenOption = config.chosenOption;
        return chosenOption.getHasGlassColor();
    }

    @Override
    protected FullDyeColor getGlassColor(StructureConfiguration configuration) {
        BasicStructureConfiguration config = (BasicStructureConfiguration) configuration;
        return config.glassColor;
    }
}
