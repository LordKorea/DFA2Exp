package nge.lk.stuff.dfa2exp;

import nge.lk.stuff.dfa2exp.model.DFA;
import nge.lk.stuff.dfa2exp.model.State;
import nge.lk.stuff.dfa2exp.transform.EquationSystem;

public final class DivisionExpressionGenerator {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar nameOfJar.jar base divisor");
            return;
        }

        int base;
        int div;
        try {
            base = Integer.parseInt(args[0]);
            div = Integer.parseInt(args[1]);
        } catch (IllegalArgumentException ignored) {
            System.err.println("Please input numbers for base and divisor");
            return;
        }

        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX || div < 1) {
            System.err.println(String.format("Invalid base or invalid divisor. Allowed values are [%d..%d] for base, [1..] for div", Character.MIN_RADIX, Character.MAX_RADIX));
            return;
        }

        DFA d = new DFA(div, base);
        d.makeFinal(0);
        for (int i = 0; i < div * base; i++) {
            String str = Integer.toString(i, base).toUpperCase();
            char[] data = str.toCharArray();

            State cursor = d.createRun();
            for (int j = 0; j < data.length; j++) {
                int symbol = getHighBaseSymbolIndex(data[j]);
                if (j == data.length - 1 && !cursor.hasTransition(symbol)) {
                    cursor.createTransition(symbol, d.getState(i % div));
                } else {
                    cursor = cursor.takeTransition(symbol);
                }
            }
        }

        EquationSystem equSys = new EquationSystem(d, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        System.out.println(equSys.solve());
    }

    private static int getHighBaseSymbolIndex(char c) {
        return c <= '9' ? c - '0' : c - 'A' + 10;
    }

    private DivisionExpressionGenerator() {
    }
}
