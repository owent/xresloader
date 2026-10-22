package org.xresloader.core;

import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;
import org.xresloader.core.engine.SharedCache;

@JCStressTest
@Outcome(id = {"0, 3, 2", "3, 0, 2"}, expect = Expect.ACCEPTABLE,
        desc = "A failed load is released; the other caller publishes a complete retry")
@Outcome(expect = Expect.FORBIDDEN, desc = "Failure poisoned the cache or corrupted publication")
@State
public class CacheFailureRecoveryStress {
    private final SharedCache<String, int[]> cache = new SharedCache<>();
    private final AtomicInteger loads = new AtomicInteger();

    private int read() {
        try {
            int[] value = cache.getOrLoad("input", () -> {
                if (loads.incrementAndGet() == 1) {
                    throw new IllegalStateException("retry");
                }
                return new int[] {1, 2};
            });
            return value[0] + value[1];
        } catch (IllegalStateException expected) {
            return 0;
        }
    }

    @Actor public void first(III_Result result) { result.r1 = read(); }
    @Actor public void second(III_Result result) { result.r2 = read(); }
    @Arbiter public void check(III_Result result) { result.r3 = loads.get(); }
}
