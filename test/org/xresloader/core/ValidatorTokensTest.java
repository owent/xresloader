package org.xresloader.core;

import org.junit.jupiter.api.Test;
import org.xresloader.core.data.vfy.DataVerifyImpl;

public class ValidatorTokensTest {

  @Test
  public void IdentityTest() {
    var vfys = DataVerifyImpl.buildValidators("\"test.a.b\"  | b.c|d|gf");
    assert vfys.size() == 4;
    assert vfys.get(0).name.equals("test.a.b");
    assert vfys.get(1).name.equals("b.c");
    assert vfys.get(2).name.equals("d");
    assert vfys.get(3).name.equals("gf");
  }

  @Test
  public void RangeTest() {
    var vfys = DataVerifyImpl.buildValidators("\">= 123\"  | <456 |-9 - -10|0-100");
    assert vfys.size() == 4;
    assert vfys.get(0).name.equals(">=123");
    assert vfys.get(1).name.equals("<456");
    assert vfys.get(2).name.equals("-9--10");
    assert vfys.get(3).name.equals("0-100");
  }

  @Test
  public void FunctionTest() {
    var vfys = DataVerifyImpl
        .buildValidators(
            "\"InText\"(\"text.log\", 2)| InTable  (  Test.xlsx , \"Test | Sheet\", 10  )|In.Table(Test.xlsx,TestSheet,10)");
    assert vfys.size() == 3;
    assert vfys.get(0).name.equals("InText(\"text.log\",\"2\")");
    assert vfys.get(1).name.equals("InTable(\"Test.xlsx\",\"Test | Sheet\",\"10\")");
    assert vfys.get(2).name.equals("In.Table(\"Test.xlsx\",\"TestSheet\",\"10\")");
  }
}
