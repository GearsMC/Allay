package org.allaymc.api.entity.data;

import lombok.Getter;

/**
 * Bir sulfur kupunun emdigi bloga gore aldigi fiziksel kisilik.
 *
 * <p>Sulfur kupunu balcik ve magma kupunden ayiran sey bu: emdigi blok yalnizca gorunumunu degil
 * nasil hareket ettigini de degistiriyor. Kup blok tasirken vurulunca firlatilip bir top gibi
 * sekiyor, ve nasil sekecegine burasi karar veriyor.</p>
 *
 * <p>Degerlerin tamami Mojang'in resmi davranis paketindeki {@code sulfur_cube.json} dosyasindan
 * birebir alindi. Hiz burada yok, cunku resmi tanimda hareket hizi kisilige degil <em>boyuta</em>
 * bagli (kucuk 0.3, buyuk 0.4); kisiliklerin hizli ya da yavas hissettirmesi surtunme ve hava
 * direncinden geliyor.</p>
 *
 * <p><strong>Surtunme ve hava direnci mutlak deger degil carpandir.</strong> Resmi tanimda
 * bunlarin adi {@code friction_modifier} ve {@code air_drag_modifier}; blok tasimayan kupte ikisi
 * de {@code 1.0}, yani "normal". Motorun kendi varsayilaniyla carpilmadan dogrudan kullanilirlarsa
 * kisilikler birbirine benzer: buzun {@code 0.05}'i motorun yerdeki {@code 0.09} varsayilanina cok
 * yakin durur ve kup hic kaymaz.</p>
 */
@Getter
public enum SulfurCubeArchetype {

    /**
     * Bos kup: hicbir blok emilmemis.
     *
     * <p>Sabitlerin sirasi onemli. Bu enum ayni zamanda istemciye
     * {@code minecraft:sulfur_cube_archetype} entity property'si olarak gonderiliyor ve istemci
     * degeri <em>sirasina</em> gore cozuyor; sira resmi tanimdaki listeyle birebir ayni.</p>
     */
    NONE(0.0f, 0.0f, 1.0f, 1.0f, false),
    /** Lastik top: cok sekiyor ve vurulunca uzaga firliyor. Tahta ve bambu bloklari. */
    BOUNCY(-2.0f, 0.9f, 0.3f, 0.01f, true),
    /** Futbol topu: dengeli. Toprak, cimen, kil, beton tozu, komur blogu. */
    REGULAR(-1.0f, 0.5f, 0.3f, 0.1f, true),
    /** Yavas ama sekiyor. Tas ailesinin cogu, tugla, kumtasi, terracotta, cevherler. */
    SLOW_BOUNCY(0.4f, 0.6f, 0.3f, 0.05f, false),
    /** Saglik topu: agir ve olu. Metal bloklari, netherite, bakir ailesi. */
    SLOW_FLAT(0.5f, 0.4f, 0.4f, 0.1f, false),
    /** Golf topu: az sekiyor ama kolay kayiyor. Mercan, sunger, balkabagi, kavun, saman. */
    FAST_FLAT(-1.0f, 0.5f, 0.2f, 0.01f, false),
    /** Plaj topu: en cok seken ama havada neredeyse duran. Butun yun renkleri. */
    LIGHT(-1.0f, 1.0f, 0.3f, 1.8f, true),
    /** Hokey diski: neredeyse hic sekmiyor, buz gibi kayiyor. Buz ve kar bloklari. */
    FAST_SLIDING(0.5f, 0.1f, 0.05f, 0.01f, false),
    /** Curling tasi: kayiyor ama yerinden zor oynuyor. Mantar bloklari, miselyum, nether mantari. */
    SLOW_SLIDING(0.8f, 0.1f, 0.05f, 0.01f, false),
    /** Yapiskan: hic sekmiyor, yere yapisiyor, ama vurulunca uzaga gidiyor. Petek blogu. */
    STICKY(-2.0f, 0.0f, 2.0f, 0.01f, false),
    /** Yerinden oynatilmasi en zor olan. Ruh kumu ve ruh topragi. */
    HIGH_RESISTANCE(0.7f, 0.2f, 1.0f, 0.01f, false),
    /** Ateslenebilir ve patlayabilir. Yalnizca TNT. */
    EXPLOSIVE(-1.0f, 0.5f, 0.3f, 0.3f, true),
    /** Dokundugu varliklara magma blogu gibi hasar verir. Yalnizca magma blogu. */
    HOT(-1.0f, 0.5f, 0.3f, 0.1f, true);

    /**
     * Geri tepme direnci.
     *
     * <p>Resmi tanimda bu deger <strong>negatif olabiliyor</strong> ve negatif olmasi direncin
     * tersi anlamina geliyor: kup vurulunca daha da uzaga firliyor. Lastik topun {@code -2.0}
     * olmasi tesadüf degil, mobun butun eglencesi orada.</p>
     */
    private final float knockbackResistance;

    /** Yere carptiginda dikey hizinin ne kadarini geri kazandigi. */
    private final float bounciness;

    /**
     * Yer surtunmesi <em>carpani</em>. Motorun varsayilan yer surtunmesiyle carpilir: {@code 1.0}
     * normal, buzun {@code 0.05}'i kupu neredeyse hic yavaslatmaz, yapiskanin {@code 2.0}'si iki
     * kati yavaslatir.
     */
    private final float frictionModifier;

    /**
     * Hava direnci <em>carpani</em>. Yine motorun varsayilaniyla carpilir; plaj topunun
     * {@code 1.8}'i onu havada belirgin sekilde yavaslatir.
     */
    private final float airDragModifier;

    /**
     * Kupun sivi uzerinde yuzup yuzmedigi.
     *
     * <p>Resmi tanimda bu ayri bir bilesen ({@code minecraft:buoyant}) ve yalnizca bes kisilikte
     * bulunuyor; sekme degerinden turetilemiyor. Yuzen kupler suda da lavda da batmaz.</p>
     */
    private final boolean buoyant;

    SulfurCubeArchetype(float knockbackResistance, float bounciness, float frictionModifier,
                        float airDragModifier, boolean buoyant) {
        this.knockbackResistance = knockbackResistance;
        this.bounciness = bounciness;
        this.frictionModifier = frictionModifier;
        this.airDragModifier = airDragModifier;
        this.buoyant = buoyant;
    }

    /**
     * @return motorun kabul ettigi araliga ({@code 0}-{@code 1}) kirpilmis geri tepme direnci.
     * Negatif degerler burada sifira duser; onlarin "daha uzaga firlat" etkisi
     * {@link #getLaunchMultiplier()} uzerinden uygulanir.
     */
    public float getClampedKnockbackResistance() {
        return Math.max(0, knockbackResistance);
    }

    /**
     * @return firlatma gucu carpani. Negatif direnc, kupun normalden daha uzaga savrulmasi demek;
     * {@code -2.0} icin bu carpan {@code 3.0} olur.
     */
    public float getLaunchMultiplier() {
        return knockbackResistance < 0 ? 1 - knockbackResistance : 1;
    }
}
