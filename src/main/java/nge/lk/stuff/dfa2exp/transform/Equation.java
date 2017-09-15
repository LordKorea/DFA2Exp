package nge.lk.stuff.dfa2exp.transform;

import nge.lk.stuff.dfa2exp.model.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a regular expression equation
 */
public class Equation {

    /**
     * The ID of this equation
     */
    private final int equationId;

    /**
     * The disjunctions of terms that represent this equation
     */
    private final List<Term> disjunctions;

    /**
     * Checks whether the given regular expression R is atomic, i.e. if (R)* is equivalent to R*
     *
     * @param expr the regular expression
     *
     * @return true if the expression is atomic
     */
    private static boolean isAtomic(String expr) {
        if (expr.length() == 1) {
            return true;
        }

        // Check whether the expression is in [] or () and there's only one top-level pair
        char[] data = expr.toCharArray();
        int depth = 0;
        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
                case '(':
                case '[':
                    depth++;
                    break;
                case ')':
                case ']':
                    depth--;
                    break;
            }
            if (depth == 0 && i != data.length - 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates an equation from a state using its transitions
     *
     * @param id the ID of this equation
     * @param state the state to use
     * @param alphabet the alphabet where transition symbols come from
     */
    public Equation(int id, State state, CharSequence alphabet) {
        equationId = id;
        disjunctions = new ArrayList<>();
        for (int i = 0; i < state.symbolCount(); i++) {
            State target = state.takeTransition(i);
            if (target != null) {
                disjunctions.add(new Term(alphabet.charAt(i), target.getStateId()));
            }
        }
        if (state.isFinal()) {
            disjunctions.add(new Term());
        }

        mergeTerms();

        // Invariant: There is at most one term per state variable.
    }

    /**
     * Applies Arden's Lemma to this equation
     * <p>
     * Arden's Lemma states that for regular languages A, B with B being not empty the following holds:
     * X = AX + B  implies  X = A*B
     */
    public void applyArdensLemma() {
        // Invariant: There is at most one term per state variable.

        Term recursiveTerm = disjunctions.stream().filter(t -> t.getStateVariable() == equationId).findFirst().orElse(null);
        if (recursiveTerm == null) {
            // Precondition for Arden's Lemma is not satisfied
            return;
        }

        // Transform the prefix into (prefix)*
        String prefix = recursiveTerm.getPrefix();
        if (!isAtomic(prefix)) {
            prefix = "(" + prefix + ")";
        }
        prefix += "*";

        // Modify the other terms (distributivity) and add everything back into the disjunction list
        Collection<Term> tmp = new LinkedList<>();
        for (Term t : disjunctions) {
            if (t != recursiveTerm) {
                tmp.add(new Term(prefix + t.getPrefix(), t.getStateVariable()));
            }
        }
        disjunctions.clear();
        disjunctions.addAll(tmp);

        // Invariant still holds: There is at most one term per state variable.
    }

    /**
     * Performs state variable substitution and splits up the terms using distributivity laws.
     * <p>
     * This function WILL VIOLATE the invariant that every state variable has at most one term. This invariant
     * has to be restored by merging
     *
     * @param value the equation which is substituted in this equation
     */
    public void substitute(Equation value) {
        // Invariant: There is at most one term per state variable.

        Term targetTerm = disjunctions.stream().filter(t -> t.getStateVariable() == value.equationId).findFirst().orElse(null);
        if (targetTerm == null) {
            // Nothing to substitute in this equation.
            return;
        }

        // This is already in parenthesis (if needed for the expression)
        String prefix = targetTerm.getPrefix();

        // Add untouched terms
        Collection<Term> tmp = new LinkedList<>();
        for (Term t : disjunctions) {
            if (t != targetTerm) {
                tmp.add(t);
            }
        }

        // Add substitutions
        for (Term t : value.disjunctions) {
            tmp.add(new Term(prefix + t.getPrefix(), t.getStateVariable()));
        }
        disjunctions.clear();
        disjunctions.addAll(tmp);

        // Invariant is VIOLATED: There might be more than one term for some state variables.
    }

    /**
     * Merges terms with the same state variable and restores the invariant
     */
    public void mergeTerms() {
        // Invariant is VIOLATED: There might be more than one term for some state variables.

        Collection<Term> tmp = new LinkedList<>();
        Set<Integer> variables = disjunctions.stream().map(Term::getStateVariable).collect(Collectors.toSet());
        for (int variable : variables) {
            Set<String> prefixes = disjunctions.stream().filter(t -> t.getStateVariable() == variable).map(Term::getPrefix).collect(Collectors.toSet());

            String combination;
            if (prefixes.size() == 1) {
                combination = prefixes.stream().findAny().get();
            } else if (prefixes.stream().allMatch(p -> p.length() == 1)) {
                combination = "[" + String.join("", prefixes) + "]";
            } else {
                combination = "(" + String.join("|", prefixes) + ")";
            }
            tmp.add(new Term(combination, variable));
        }
        disjunctions.clear();
        disjunctions.addAll(tmp);

        // Invariant restored: There is at most one term per state variable.
    }

    /**
     * Converts this equation to a regular expression if that is possible
     *
     * @return the regular expression
     */
    public String toExpression() {
        boolean free = disjunctions.size() == 1 && disjunctions.get(0).getStateVariable() == -1;
        assert free : "Variables left in result";

        return disjunctions.get(0).getPrefix();
    }

    @Override
    public String toString() {
        return String.format("EQ %d: %s", equationId, String.join("\n  ", disjunctions.stream().map(t -> String.format("[%d] %s", t.getStateVariable(), t.getPrefix())).collect(Collectors.toList())));
    }

    /**
     * @return the ID of the equation
     */
    public int getEquationId() {
        return equationId;
    }
}
