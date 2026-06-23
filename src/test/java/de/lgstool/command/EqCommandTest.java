package de.lgstool.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.math.Fraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EqCommandTest {

    private CommandEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CommandEngine();
        engine.execute("create(t, 2*2);", CommandContext.main());
    }

    private CommandResult eq(String line) {
        return engine.execute(line, CommandContext.lgs("t"));
    }

    @Test
    @DisplayName("eq writes natural-form coefficients into the matrix")
    void writesCoefficients() {
        assertTrue(eq("eq(2x + 3y = 5);").isOk());
        Fraction[] row = engine.state().get("t").row(1);
        assertEquals(Fraction.of(2), row[0]);
        assertEquals(Fraction.of(3), row[1]);
        assertEquals(Fraction.of(5), row[2]);
    }

    @Test
    @DisplayName("eq keeps exact fractions and overwrites a chosen row")
    void exactAndOverwrite() {
        eq("eq(2x + 3y = 5);");
        assertTrue(eq("eq(x - 1/2 y = 1, 2);").isOk());
        Fraction[] row = engine.state().get("t").row(2);
        assertEquals(Fraction.ONE, row[0]);
        assertEquals(Fraction.of(-1, 2), row[1]);
        assertEquals(Fraction.ONE, row[2]);
    }

    @Test
    @DisplayName("eq reports unknown variables and is rejected outside an LGS")
    void errors() {
        assertFalse(eq("eq(2x + w = 1);").isOk());            // unknown variable w
        assertFalse(engine.execute("eq(2x + 3y = 5);", CommandContext.main()).isOk());
    }
}
