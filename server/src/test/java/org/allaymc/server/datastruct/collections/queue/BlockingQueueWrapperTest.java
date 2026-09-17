package org.allaymc.server.datastruct.collections.queue;

import io.netty.util.internal.PlatformDependent;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author daoge_cmd
 */
class BlockingQueueWrapperTest {
    @SneakyThrows
    @Test
    void test() {
        var queue = BlockingQueueWrapper.wrap(PlatformDependent.newMpscQueue());
        AtomicInteger integer = new AtomicInteger(0);

        // GearsMC: tüketici eskiden "running || size != 0" koşuluyla dönüyordu. 100 öğeyi running daha true iken
        // bitirirse bir sonraki poll() sonsuza kadar bekliyor ve test (CI dahil) asılı kalıyordu. Tüketici artık tam
        // 100 öğe alır; üretici gecikmeli yazdığı için boş kuyrukta bekleme yine sınanır.
        var thread = Thread.ofPlatform().start(() -> {
            for (int i = 0; i < 100; i++) {
                // Should block here if queue is empty
                queue.poll();
                integer.incrementAndGet();
            }
        });

        for (int i = 0; i < 100; i++) {
            queue.offer(i);
            if (i % 25 == 0) {
                Thread.sleep(5);
            }
        }
        thread.join();
        assertEquals(100, integer.get());
        assertNull(queue.pollNow());
    }
}