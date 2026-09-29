package org.allaymc.server.network.processor.ingame;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.component.BlockRespawnPointComponent;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.player.PlayerRespawnEvent;
import org.allaymc.api.math.location.Location3ic;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;
import org.allaymc.server.network.processor.PacketProcessor;
import org.allaymc.server.player.AllayPlayer;
import org.allaymc.server.world.AllayDimension;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.joml.Vector3i;

/**
 * @author IWareQ | daoge_cmd
 */
@Slf4j
public class RespawnPacketProcessor extends PacketProcessor<RespawnPacket> {

    @Override
    public void handleSync(Player player, RespawnPacket packet, long receiveTime) {
        if (packet.getState() != RespawnPacket.State.CLIENT_READY) {
            log.warn("Respawn state must be CLIENT_READY, but got {}", packet.getState());
            return;
        }

        var entity = player.getControlledEntity();
        if (!entity.canBeSpawned()) {
            if (entity.isAlive() && entity.getHealth() > 0) {
                // Sunucuda oyuncu zaten yeniden dogmus ama istemci hala olum ekraninda (ör. dogustan
                // sonra 0 can gordu). Yoksaymak ekrani kilitliyordu; istemciyi bulundugu yerde
                // yeniden dogur ve gercek cani tekrar gonder.
                var allayPlayer = (AllayPlayer) player;
                var location = entity.getLocation();
                allayPlayer.sendPacket(allayPlayer.getProtocol().getEncoder().encodeRespawn(new Vector3i(
                        (int) Math.floor(location.x()), (int) Math.floor(location.y()), (int) Math.floor(location.z()))));
                allayPlayer.sendHealth(entity.getHealth(), entity.getMaxHealth());
            }
            // Wait until the entity can be spawned again
            return;
        }

        var event = new PlayerRespawnEvent(entity);
        event.setRespawnLocation(this.findSpawnPoint(entity));
        event.call();

        var spawnPoint = event.getRespawnLocation();

        // NOTICE: No need to set runtime entity id
        var allayPlayer = (AllayPlayer) player;
        allayPlayer.sendPacket(allayPlayer.getProtocol().getEncoder().encodeRespawn(spawnPoint));

        // Remove the player from the dimension first to properly clean up chunk loader and entity
        // viewer state, then re-add the player to respawn
        var dimension = (AllayDimension) entity.getDimension();
        dimension.removePlayer(player, () -> {
            dimension.addPlayer(player, () -> {
                resetData(entity);
                entity.teleport(spawnPoint);
            });
        });
    }

    private Location3ic findSpawnPoint(EntityPlayer entity) {
        var spawnPoint = entity.validateAndGetSpawnPoint();
        var dimension = spawnPoint.dimension();

        var blockState = dimension.getBlockState(spawnPoint.x(), spawnPoint.y(), spawnPoint.z());
        var blockBehavior = blockState.getBlockType().getBlockBehavior();
        if (blockBehavior instanceof BlockRespawnPointComponent respawnPointComponent) {
            var respawnLocation = respawnPointComponent.onPlayerRespawn(entity, new Block(blockState, new Position3i(spawnPoint, dimension)));
            if (respawnLocation != null) {
                return respawnLocation;
            }

            var globalSpawnPoint = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
            entity.setSpawnPoint(globalSpawnPoint);
            return globalSpawnPoint;
        }

        // Block-anchored spawn points (bed/respawn anchor) that no longer have a valid block
        // should fall back to world spawn with an appropriate message
        var source = entity.getSpawnPointType();
        if (source.invalidSpawnKey != null) {
            entity.sendTranslatable(source.invalidSpawnKey);
            var globalSpawnPoint = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
            entity.setSpawnPoint(globalSpawnPoint);
            return globalSpawnPoint;
        }

        return spawnPoint;
    }

    private void resetData(EntityPlayer player) {
        // Can efektlerden once doldurulur: saglik artisi kaldirilirken azami can degisince istemciye
        // can ozelligi gonderiliyor; can o an hala 0 oldugu icin istemci yeniden dogar dogmaz
        // tekrar olum ekranini aciyordu.
        player.resetHealth();
        player.removeAllEffects();
        player.resetFoodData();
        player.extinguish();
        player.setAirSupplyTicks(player.getAirSupplyMaxTicks());
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.RESPAWN;
    }
}
