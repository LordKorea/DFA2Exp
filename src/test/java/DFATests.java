import nge.lk.stuff.dfa2exp.model.DFA;
import nge.lk.stuff.dfa2exp.model.State;
import nge.lk.stuff.dfa2exp.transform.EquationSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DFATests {

    @Test
    public void testDFACompleteness() {
        DFA dfa = new DFA(2, 2);
        dfa.makeFinal(0);

        dfa.getState(0).createTransition(0, dfa.getState(0));
        dfa.getState(0).createTransition(1, dfa.getState(1));
        dfa.getState(1).createTransition(1, dfa.getState(1));
        dfa.getState(1).createTransition(0, dfa.getState(0));

        Assertions.assertTrue(dfa.isComplete(), "DFA is not complete");
    }

    @Test
    public void testDivisionAutomata() {
        Random r = new Random();
        String fullAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int base = Character.MIN_RADIX; base <= Character.MAX_RADIX; base++) {
            for (int div = 2; div < 8; div++) {
                DFA dfa = new DFA(div, base);
                dfa.makeFinal(0);
                for (int i = 0; i < div * base; i++) {
                    String str = Integer.toString(i, base).toUpperCase();
                    char[] data = str.toCharArray();

                    State cursor = dfa.createRun();
                    for (int j = 0; j < data.length; j++) {
                        int symbol = getHighBaseSymbolIndex(data[j]);
                        if (j == data.length - 1 && !cursor.hasTransition(symbol)) {
                            cursor.createTransition(symbol, dfa.getState(i % div));
                        } else {
                            Assertions.assertTrue(cursor.hasTransition(symbol), String.format("(BASE, DIV) = (%d, %d): Invalid transition prefix of %s at index %d", base, div, str, j));
                            cursor = cursor.takeTransition(symbol);
                            if (j == data.length - 1) {
                                Assertions.assertEquals(cursor, dfa.getState(i % div), String.format("(BASE, DIV) = (%d, %d): DFA does not produce correct sequence for %s, ends in wrong state", base, div, str));
                            }
                        }
                    }
                }
                Assertions.assertTrue(dfa.isComplete(), String.format("(BASE, DIV) = (%d, %d): DFA is not complete", base, div));

                EquationSystem system = new EquationSystem(dfa, fullAlphabet.substring(0, base));
                String expr = system.solve();
                Matcher m = Pattern.compile(expr).matcher("");
                for (int i = 0; i < 100; i++) {
                    int num = r.nextInt(10000);
                    m.reset(Integer.toString(num, base).toUpperCase());
                    Assertions.assertEquals(num % div == 0, m.matches(), String.format("(BASE, DIV) = (%d, %d): Expression does not return correct result for %d", base, div, num));
                }
            }
        }
    }

    private static int getHighBaseSymbolIndex(char c) {
        return c <= '9' ? c - '0' : c - 'A' + 10;
    }
}
