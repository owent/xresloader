package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.xresloader.core.data.vfy.DataValidatorFactory;
import org.xresloader.core.data.vfy.DataVerifyImpl.ValidatorTokens;

public class ValidatorTokensTest {

  @Test
  public void IdentityTest() {
    var vfys = DataValidatorFactory.buildValidatorTokens("\"test.a.b\"  | b.c|d|gf");
    assert vfys.size() == 4;
    assert vfys.get(0).toString().equals("test.a.b");
    assert vfys.get(1).toString().equals("b.c");
    assert vfys.get(2).toString().equals("d");
    assert vfys.get(3).toString().equals("gf");
  }

  @Test
  public void RangeTest() {
    var vfys = DataValidatorFactory.buildValidatorTokens("\">= 123\"  | <456 |-9 - -10|0-100");
    assert vfys.size() == 4;
    assert vfys.get(0).toString().equals(">=123");
    assert vfys.get(1).toString().equals("<456");
    assert vfys.get(2).toString().equals("-9--10");
    assert vfys.get(3).toString().equals("0-100");
  }

  @Test
  public void FunctionTest() {
    var vfys = DataValidatorFactory
        .buildValidatorTokens(
            "\"InText\"(\"text.log\", 2)| InTable  (  Test.xlsx , \"Test | Sheet\", 10  )|In.Table(Test.xlsx,TestSheet,10)");
    assert vfys.size() == 3;
    assert vfys.get(0).toString().equals("InText(\"text.log\",\"2\")");
    assert vfys.get(1).toString().equals("InTable(\"Test.xlsx\",\"Test | Sheet\",\"10\")");
    assert vfys.get(2).toString().equals("In.Table(\"Test.xlsx\",\"TestSheet\",\"10\")");
  }

  @Test
  public void NullExpressionReturnsNull() {
    assertNull(DataValidatorFactory.buildValidatorTokens(null));
  }

  @Test
  public void WhitespaceExpressionReturnsEmptyList() {
    var vfys = DataValidatorFactory.buildValidatorTokens("   \t  \r\n   ");
    assertNotNull(vfys);
    assertTrue(vfys.isEmpty());
  }

  @Test
  public void ConsecutiveAndTrailingPipesIgnored() {
    var vfys = DataValidatorFactory.buildValidatorTokens("foo||bar| |baz|");
    assertEquals(3, vfys.size());
    assertEquals("foo", vfys.get(0).toString());
    assertEquals("bar", vfys.get(1).toString());
    assertEquals("baz", vfys.get(2).toString());
  }

  @Test
  public void NestedFunctionsPreserveStructure() {
    var vfys = DataValidatorFactory
        .buildValidatorTokens("Outer(InnerOne(\"v1\"),InnerTwo(InnerThree(123)))|Simple");
    assertEquals(2, vfys.size());

    ValidatorTokens outer = vfys.get(0);
    assertEquals("Outer(InnerOne(\"v1\"),InnerTwo(InnerThree(\"123\")))", outer.toString());

    var params = outer.getParameters();
    assertEquals(3, params.size());
    assertEquals("Outer", params.get(0).getStringValue());
    assertTrue(params.get(1).isToken());
    assertEquals("InnerOne(\"v1\")", params.get(1).getTokens().toString());
    assertTrue(params.get(2).isToken());
    assertEquals("InnerTwo(InnerThree(\"123\"))", params.get(2).getTokens().toString());

    assertEquals("Simple", vfys.get(1).toString());
  }

  @Test
  public void PipeInsideStringStaysLiteral() {
    var vfys = DataValidatorFactory.buildValidatorTokens("InText(\"a|b,c\")|'dangling|pipe'");
    assertEquals(2, vfys.size());
    assertEquals("InText(\"a|b,c\")", vfys.get(0).toString());
    assertEquals("dangling|pipe", vfys.get(1).toString());
  }
}
