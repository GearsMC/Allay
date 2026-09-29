package org.allaymc.server.item.component;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityCushion;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.item.EntityCushionBaseComponentImpl;
import org.joml.Vector3ic;
import org.joml.primitives.AABBd;

public class ItemCushionBaseComponentImpl extends ItemBaseComponentImpl {

    public ItemCushionBaseComponentImpl(ItemStackInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public boolean useItemOnBlock(Dimension dimension, Vector3ic placeBlockPos, PlayerInteractInfo interactInfo) {
        if (super.useItemOnBlock(dimension, placeBlockPos, interactInfo)) {
            return true;
        }
        if (interactInfo == null) {
            return false;
        }

        var player = interactInfo.player();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        var blockState = dimension.getBlockState(placeBlockPos);
        var type = blockState.getBlockType();
        if (!type.hasBlockTag(BlockTags.REPLACEABLE) || type.hasBlockTag(BlockTags.WATER) || type.hasBlockTag(BlockTags.LAVA)) {
            return false;
        }

        int x = placeBlockPos.x();
        int y = placeBlockPos.y();
        int z = placeBlockPos.z();
        if (!EntityCushionBaseComponentImpl.supportsCushion(dimension.getBlockState(x, y - 1, z))) {
            return false;
        }

        var cell = new AABBd(x, y, z, x + 1, y + 1, z + 1);
        for (var entity : dimension.getEntityManager().getPhysicsService().computeCollidingEntities(cell)) {
            if (entity instanceof EntityCushion) {
                return false;
            }
        }

        var cushion = EntityTypes.CUSHION.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(x + 0.5, y, z + 0.5)
                .rot(0, 0)
                .build());
        cushion.setColor(EntityCushionBaseComponentImpl.colorOf(thisItemStack.getItemType()));
        dimension.getEntityManager().addEntity(cushion);
        dimension.addSound(cushion.getLocation(), new CustomSound(SoundNames.ENTITY_CUSHION_PLACE));
        player.tryConsumeItemInHand();
        return true;
    }
}
