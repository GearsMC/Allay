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
     * The player editing the sign.
     */
    protected EntityPlayer editor;
    /**
     * Duzenlenen yuzun degisiklikten onceki metni.
     *
     * <p>GearsMC fork eki — PocketMine {@code SignChangeEvent::getOldText()} karsiligi.
     * Motor olayi metni yazmadan ONCE atesledigi icin bu dizi blok varliginin o anki
     * icerigidir. Kurucuda kopyalanir; degistirmek tabelayi etkilemez. Eski metin
     * bilinmiyorsa bos dizidir.</p>
     */
    protected String[] oldText;
    /**
     * Duzenlenen yuzun on yuz olup olmadigi.
     *
     * <p>GearsMC fork eki — PocketMine {@code SignChangeEvent} olayindaki
     * {@code frontFace} karsiligi.</p>
     */
    protected boolean frontSide;

    public SignTextChangeEvent(Block block, String[] text, EntityPlayer editor) {
        this(block, text, editor, new String[0], true);
    }

    /**
     * Eski metni ve duzenlenen yuzu de tasiyan kurucu (GearsMC fork eki).
     *
     * @param block     tabela blogu
     * @param text      yeni metin (en fazla 4 satir)
     * @param editor    duzenleyen oyuncu
     * @param oldText   duzenlenen yuzun degisiklikten onceki metni
     * @param frontSide duzenlenen yuz on yuz mu
     */
    public SignTextChangeEvent(Block block, String[] text, EntityPlayer editor, String[] oldText, boolean frontSide) {
        super(block);
        setText(text);
        this.editor = editor;
        this.oldText = oldText == null ? new String[0] : oldText.clone();
        this.frontSide = frontSide;
    }

    public void setText(String[] text) {
        if (text.length > 4) {
            throw new IllegalArgumentException("Sign text must be 4 lines or less");
        }
        this.text = text;
    }
}
