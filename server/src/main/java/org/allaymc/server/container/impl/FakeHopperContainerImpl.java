package org.allaymc.server.container.impl;

import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.player.Player;
import org.allaymc.server.blockentity.data.BlockEntityId;
import org.allaymc.server.player.AllayPlayer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.joml.Vector3ic;

import static org.allaymc.server.network.NetworkHelper.toNetwork;

/**
 * @author daoge_cmd
 */
public class FakeHopperContainerImpl extends FakeContainerImpl {
    public FakeHopperContainerImpl() {
        super(ContainerTypes.FAKE_HOPPER);
    }

    @Override
    protected void sendFakeBlocks(Player player) {
        var pos = computeFakeBlockPos(player);
        player.viewBlockUpdate(pos, 0, BlockTypes.HOPPER.getDefaultState());

        var nbt = NbtMap.builder()
                .putString("id", BlockEntityId.HOPPER)
                .putInt("x", pos.x())
                .putInt("y", pos.y())
                .putInt("z", pos.z());

        if (this.customName != null) {
            nbt.putString("CustomName", this.customName);
        }

        // multi-version v2 sonrasi paket burada kurulmaz: her protokol surumu kendi
        // kodlayicisiyla uretir, sendPacket de API'den cikip sunucu icinde kaldi.
        var allayPlayer = (AllayPlayer) player;
        allayPlayer.sendPacket(allayPlayer.getProtocol().getEncoder()
                .encodeBlockEntityData(pos, nbt.build()));

        this.fakeBlockPositions.put(player, new Vector3ic[]{pos});
    }
}
