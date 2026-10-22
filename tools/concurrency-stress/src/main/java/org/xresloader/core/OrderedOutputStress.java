package org.xresloader.core;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "The later input is always the last writer")
@Outcome(expect = Expect.FORBIDDEN, desc = "Output order or visibility violated")
@State
public class OrderedOutputStress {
    private final TaskOrder first = new TaskOrder(null);
    private final TaskOrder second = new TaskOrder(first);
    private int output;

    @Actor public void first() {
        first.enter();
        try {
            TaskOrder.awaitOutputTurn();
            output = 1;
        } finally { first.finish(); }
    }

    @Actor public void second() {
        second.enter();
        try {
            TaskOrder.awaitOutputTurn();
            output = 2;
        } finally { second.finish(); }
    }

    @Arbiter public void check(I_Result result) { result.r1 = output; }
}
