package org.xresloader.core;

import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;
import org.xresloader.core.engine.SharedCache;

@JCStressTest
@Outcome(id = "3, 3, 1", expect = Expect.ACCEPTABLE, desc = "One complete value published to both readers")
@Outcome(expect = Expect.FORBIDDEN, desc = "Incomplete publication or duplicate load")
@State
public class CachePublicationStress {
    private final SharedCache<String, int[]> cache = new SharedCache<>();
    private final AtomicInteger loads = new AtomicInteger();

    private int read() {
        int[] value = cache.getOrLoad("input", () -> {
            loads.incrementAndGet();
            return new int[] {1, 2};
        });
        return value[0] + value[1];
    }

    @Actor public void first(III_Result result) { result.r1 = read(); }
    @Actor public void second(III_Result result) { result.r2 = read(); }
    @Arbiter public void check(III_Result result) { result.r3 = loads.get(); }
}
