package org.xresloader.core;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

@JCStressTest
@Outcome(id = "1, 2", expect = Expect.ACCEPTABLE, desc = "Each singleton accessor retains its task options")
@Outcome(expect = Expect.FORBIDDEN, desc = "Options overwritten by another actor")
@State
public class OptionsIsolationStress {
    private int read(int value) {
        try {
            ProgramOptions.getInstance().parallelism = value;
            Thread.yield();
            return ProgramOptions.getInstance().parallelism;
        } finally {
            ProgramOptions.clearThreadState();
        }
    }

    @Actor public void first(II_Result result) { result.r1 = read(1); }
    @Actor public void second(II_Result result) { result.r2 = read(2); }
}
