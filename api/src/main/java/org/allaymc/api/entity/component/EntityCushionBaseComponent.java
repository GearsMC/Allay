package org.allaymc.api.entity.component;

import org.allaymc.api.utils.DyeColor;

public interface EntityCushionBaseComponent extends EntityBaseComponent {

    DyeColor getColor();

    void setColor(DyeColor color);
}
