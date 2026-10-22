package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.xresloader.core.data.vfy.DataValidatorCache;
import org.xresloader.core.data.vfy.DataVerifyNumberRange;

class DataValidatorCacheTest {
    @Test
    void pendingValidatorsKeepInsertionOrderAndIgnoreRepeatedInstances() {
        var cache = new DataValidatorCache();
        var first = new DataVerifyNumberRange("0-10");
        var second = new DataVerifyNumberRange("11-20");
        var replacement = new DataVerifyNumberRange("21-30");

        cache.putValidator("first", first);
        cache.putValidator("first", first);
        cache.putValidator("second", second);
        cache.putValidator("first", replacement);

        assertSame(replacement, cache.getValidatorByKey("first"));
        assertSame(first, cache.popUnsetupValidator());
        assertSame(second, cache.popUnsetupValidator());
        assertSame(replacement, cache.popUnsetupValidator());
        assertNull(cache.popUnsetupValidator());
    }

    @Test
    void invalidEntriesDoNotEnterThePendingQueue() {
        var cache = new DataValidatorCache();
        cache.putValidator((String) null, new DataVerifyNumberRange("0-10"));
        cache.putValidator("", new DataVerifyNumberRange("0-10"));
        cache.putValidator("missing", null);
        assertNull(cache.popUnsetupValidator());
        assertNull(cache.getValidatorByKey("missing"));
    }
}
