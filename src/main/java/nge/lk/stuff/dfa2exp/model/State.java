package nge.lk.stuff.dfa2exp.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a state of a deterministic finite automaton
 */
public class State {

    /**
     * The ID of this state
     */
    private final int stateId;

    /**
     * The successors of this state, indexed by symbol
     */
    private final State[] successors;

    /**
     * Whether this state is a final state
     */
    private boolean finalState;

    /**
     * Initializes this state with the given number of successors (symbols in the alphabet)
     *
     * @param e the number of symbols in the alphabet
     */
    public State(int id, int e) {
        stateId = id;
        successors = new State[e];
    }

    /**
     * Marks this state as a final state
     */
    public void makeFinal() {
        finalState = true;
    }

    /**
     * Checks whether this state has a transition for the given input symbol
     *
     * @param e the input symbol (by index)
     *
     * @return true if a transition exists for the symbol denoted by {@code e}
     */
    public boolean hasTransition(int e) {
        return successors[e] != null;
    }

    /**
     * Accepts the given symbol and returns the state that is the result of the transition
     *
     * @param e the input symbol (by index)
     *
     * @return the new state
     */
    public State takeTransition(int e) {
        return successors[e];
    }

    /**
     * Creates a transition for the given symbol with the given state as a target
     *
     * @param e the symbol of the transition
     * @param q the target state of the transition
     */
    public void createTransition(int e, State q) {
        assert successors[e] == null : "Can not override successor for symbol " + e;

        successors[e] = q;
    }

    /**
     * @return the amount of symbols in this state's transition table. Does not necessarily match the number of valid transitions
     */
    public int symbolCount() {
        return successors.length;
    }

    /**
     * Checks whether this state is complete, i.e. it has a transition for every symbol of the alphabet
     *
     * @return true if this state is complete
     */
    public boolean isComplete() {
        return Arrays.stream(successors).allMatch(Objects::nonNull);
    }

    /**
     * @return the state's ID
     */
    public int getStateId() {
        return stateId;
    }

    /**
     * @return whether this is a final state
     */
    public boolean isFinal() {
        return finalState;
    }
}
