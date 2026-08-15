package org.allaymc.api.entity.component;

/**
 * Boyutu olan kup moblari — balcik ve magma kupu — icin ortak bilesen.
 *
 * <p>Bu iki mob tek bir varlik turu olup boyutuyla degisiyor: can, hasar, carpisma kutusu ve
 * olunce kacak parcaya bolunecegi hep bu tek sayidan turuyor.</p>
 */
public interface EntityCubeBaseComponent extends EntityBaseComponent {

    /**
     * @return kupun boyutu; vanilla'da kucuk icin {@code 1}, orta icin {@code 2}, buyuk icin
     * {@code 4}
     */
    int getCubeSize();

    /**
     * Kupun boyutunu ayarlar; can, olcek ve carpisma kutusu buna gore yeniden hesaplanir.
     *
     * @param size yeni boyut
     */
    void setCubeSize(int size);
}
