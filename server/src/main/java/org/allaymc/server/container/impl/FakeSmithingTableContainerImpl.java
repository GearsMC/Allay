package org.allaymc.server.container.impl;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.container.interfaces.SmithingTableContainer;
import org.allaymc.api.item.recipe.input.RecipeInput;
import org.allaymc.api.item.recipe.input.SmithingRecipeInput;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.player.Player;
import org.joml.Vector3ic;

/**
 * Bloksuz nalbant masası sahte konteyneri (Fake Smithing Table Container).
 *
 * <p>PocketMine'daki {@code NetheriteSmithingMenu} ve {@code SmithingTableMenu}
 * mantığına paralel olarak, dünyada gerçek bir blok varlığı oluşturmadan oyuncuya
 * bloksuz nalbant masası arayüzünü açar.</p>
 *
 * <h2>Neden yeni ContainerType veya ContainerNetworkInfo açılmadı</h2>
 * <p>{@link ContainerTypes#SMITHING_TABLE} zaten kayıtlıdır ve yuvaların ağ eşlemesi
 * hazırdır. {@code AllayPlayer} sahte konteynerler için istemciye sahte blok konumunu
 * iletmekte ve konteyneri kendi tipiyle saklamaktadır. {@code CraftRecipeActionProcessor}
 * da nalbant tariflerinde {@code ContainerTypes.SMITHING_TABLE} aradığı için sahte
 * konteyner doğrudan bu tiple kurulduğunda tarif işleme hattına kendiliğinden dahil olur.</p>
 *
 * <p>Nalbant masasının sandık veya huni gibi bir blok varlığı (block entity NBT)
 * yoktur; istemciye yalnızca sahte blok güncellemesi gönderilmesi yeterlidir.</p>
 *
 * @author daoge_cmd
 */
public class FakeSmithingTableContainerImpl extends FakeContainerImpl implements SmithingTableContainer {

    /**
     * {@link SmithingTableContainer} {@code BlockContainer}'i genislettigi icin bu alan
     * arayuz sozlesmesi geregi durur; sahte konteynerin gercek bir blogu olmadigindan
     * <strong>her zaman {@code null}</strong> kalir. Istemciye gonderilen konum
     * {@code AllayPlayer.sendContainerOpenPacket} icinde sahte blok konumundan
     * ({@link #getFakeBlockPos}) alinir — o switch'te sahte konteyner dali once gelir.
     */
    @Getter
    @Setter
    protected Position3ic blockPos;

    public FakeSmithingTableContainerImpl() {
        super(ContainerTypes.SMITHING_TABLE);
    }

    @Override
    public RecipeInput createRecipeInput() {
        return new SmithingRecipeInput(getTemplate(), getInput(), getMaterial());
    }

    @Override
    protected void sendFakeBlocks(Player player) {
        var pos = computeFakeBlockPos(player);
        player.viewBlockUpdate(pos, 0, BlockTypes.SMITHING_TABLE.getDefaultState());
        this.fakeBlockPositions.put(player, new Vector3ic[]{pos});
    }
}
