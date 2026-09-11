package org.allaymc.api.eventbus.event.block;

import lombok.Getter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.CancellableEvent;

/**
 * Called when a player changes the text on a sign.
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.WORLD)
public class SignTextChangeEvent extends BlockEvent implements CancellableEvent {
    /**
     * The new sign text.
     */
    protected String[] text;
    /**
     * The text of the edited face before this change.
     */
    protected String[] oldText;
    /**
     * The player editing the sign.
     */
    protected EntityPlayer editor;

    public SignTextChangeEvent(Block block, String[] text, EntityPlayer editor) {
        this(block, text, editor, new String[0]);
    }

    public SignTextChangeEvent(Block block, String[] text, EntityPlayer editor, String[] oldText) {
        super(block);
        setText(text);
        this.editor = editor;
        this.oldText = oldText == null ? new String[0] : oldText.clone();
    }

    public void setText(String[] text) {
        if (text.length > 4) {
            throw new IllegalArgumentException("Sign text must be 4 lines or less");
        }
        this.text = text;
    }
}
