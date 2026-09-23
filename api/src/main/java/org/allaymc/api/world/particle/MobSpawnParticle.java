package org.allaymc.api.world.particle;

/**
 * Mob doğarken çıkan duman bulutu (spawner/yumurta efekti).
 *
 * <p>PocketMine {@code MobSpawnParticle} karşılığı: bulutun boyutu doğan mobun genişliği ve
 * yüksekliğiyle (blok, 0-255) verilir.</p>
 *
 * @param width  bulut genişliği (blok)
 * @param height bulut yüksekliği (blok)
 */
public record MobSpawnParticle(int width, int height) implements Particle {
}
