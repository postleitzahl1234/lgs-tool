package de.lgstool.geo.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParserTest {

    @Test
    @DisplayName("binary expression builds a Bin node")
    void parsesBinary() {
        Expr e = Parser.parse("2 + 3");
        assertInstanceOf(Expr.Bin.class, e);
        Expr.Bin b = (Expr.Bin) e;
        assertEquals('+', b.op());
    }

    @Test
    @DisplayName("bare identifier is a Ref, call is a Call")
    void parsesRefAndCall() {
        assertInstanceOf(Expr.Ref.class, Parser.parse("abc"));
        assertInstanceOf(Expr.Call.class, Parser.parse("Line(A, B)"));
    }

    @Test
    @DisplayName("comma list in parens is a Tuple, single value is grouping")
    void parsesTupleVsGroup() {
        assertInstanceOf(Expr.Tuple.class, Parser.parse("(1, 2)"));
        assertInstanceOf(Expr.Tuple.class, Parser.parse("(1, 2, 3)"));
        assertInstanceOf(Expr.Bin.class, Parser.parse("(1 + 2)"));
    }

    @Test
    @DisplayName("collectNames gathers refs and call names, not numbers")
    void collectsNames() {
        Set<String> names = new HashSet<>();
        Expr.collectNames(Parser.parse("Midpoint(A, B) + 2 * c"), names);
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));
        assertTrue(names.contains("Midpoint"));
        assertTrue(names.contains("c"));
    }

    @Test
    @DisplayName("incomplete input is rejected")
    void rejectsIncomplete() {
        assertThrows(EvalException.class, () -> Parser.parse("2 +"));
        assertThrows(EvalException.class, () -> Parser.parse("(1, 2"));
        assertThrows(EvalException.class, () -> Parser.parse("2 3"));
    }
}
