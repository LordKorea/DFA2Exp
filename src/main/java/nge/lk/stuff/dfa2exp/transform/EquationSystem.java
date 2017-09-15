package nge.lk.stuff.dfa2exp.transform;

import nge.lk.stuff.dfa2exp.model.DFA;

import java.util.Arrays;

/**
 * Represents a system of equations of regular expressions
 */
public class EquationSystem {

    /**
     * The equations in this system
     */
    private final Equation[] equations;

    /**
     * Creates an equation system from the given DFA using the given alphabet
     *
     * @param source the DFA
     * @param alphabet the alphabet to use for transforming the DFA into a system of equations
     */
    public EquationSystem(DFA source, String alphabet) {
        equations = new Equation[source.stateCount()];
        Arrays.setAll(equations, i -> new Equation(i, source.getState(i), alphabet));
    }

    /**
     * Solves this equation system (assuming the initial state is 0)
     *
     * @return the solution of this equation system as a regular expression
     */
    public String solve() {
        for (int eliminate = equations.length - 1; eliminate >= 0; eliminate--) {
            applyArdensLemma(eliminate);
            performSubstitution(equations[eliminate]);
            mergeTerms(eliminate);
        }

        // To create the final result, merge the last equation
        equations[0].mergeTerms();

        return equations[0].toExpression();
    }

    /**
     * Applies Arden's lemma to all equations that are still relevant
     *
     * @param upperLimit the upper limit for relevance (inclusive)
     */
    private void applyArdensLemma(int upperLimit) {
        // Arden's Lemma needs to be applied to the equation currently being eliminated as well
        for (int i = 0; i <= upperLimit; i++) {
            equations[i].applyArdensLemma();
        }
    }

    /**
     * Performs state variable substitution for all equations that are still relevant
     *
     * @param value the equation which should be substituted in all relevant equations
     */
    private void performSubstitution(Equation value) {
        // Invariant: No equation ">= variable" is relevant for substitution
        for (int i = 0; i < value.getEquationId(); i++) {
            equations[i].substitute(value);
        }
    }

    /**
     * Merges terms in equations that are still relevant
     *
     * @param upperLimit the upper limit for relevance (exclusive)
     */
    private void mergeTerms(int upperLimit) {
        for (int i = 0; i < upperLimit; i++) {
            equations[i].mergeTerms();
        }
    }
}
