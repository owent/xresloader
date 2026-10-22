package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.vfy.DataVerifyImpl;

class NumericConversionTest {
    @Test
    void groupedIntegersAndPercentagesKeepTheirNumericValues() throws Exception {
        assertEquals(1234, DataVerifyImpl.getAndVerifyToLong(null, null, "value", "1,234"));
        assertEquals(-1234, DataVerifyImpl.getAndVerifyToLong(null, null, "value", "-1,234"));
        // Preserve the baseline's integer-percent behavior; changing it is a separate format change.
        assertEquals(200, DataVerifyImpl.getAndVerifyToLong(null, null, "value", "200%"));
        assertEquals(0.125, DataVerifyImpl.getAndVerifyToDouble(null, null, "value", "12.5%"));
        assertEquals(-0.125, DataVerifyImpl.getAndVerifyToDouble(null, null, "value", "-12.5%"));
        assertThrows(ConvException.class,
                () -> DataVerifyImpl.getAndVerifyToLong(null, null, "value", "12.5%"));
    }

    @Test
    void sharedPatternsCreateIndependentMatchersForConcurrentConversions() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            var jobs = new ArrayList<Callable<Double>>();
            for (int index = 0; index < 100; index++) {
                int value = index;
                jobs.add(() -> DataVerifyImpl.getAndVerifyToDouble(null, null, "value", value + ".5%"));
            }
            var results = executor.invokeAll(jobs);
            for (int index = 0; index < results.size(); index++) {
                assertEquals((index + 0.5) / 100, results.get(index).get(), 1e-12);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
