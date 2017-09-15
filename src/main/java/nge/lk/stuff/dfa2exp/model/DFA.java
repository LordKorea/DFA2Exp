package nge.lk.stuff.dfa2exp.model;

import java.util.Arrays;

/**
 * Represents a deterministic finite automaton
 */
public class DFA {

    /**
     * The set of states
     */
    private final State[] states;

    /**
     * Creates a deterministic finite automaton with the given amount of states
     *
     * @param q the number of states this DFA will have
     * @param e the number of symbols in the alphabet
     */
    public DFA(int q, int e) {
        states = new State[q];
        Arrays.setAll(states, i -> new State(i, e));
    }

    /**
     * Marks the state indicated by the given index as a final state
     *
     * @param q the index of the state
     */
    public void makeFinal(int q) {
        states[q].makeFinal();
    }

    /**
     * Starts a run on this automaton
     *
     * @return the initial state of this automaton
     */
    public State createRun() {
        return states[0];
    }

    /**
     * Returns the state with the given index
     *
     * @param q the index of the state
     *
     * @return the state
     */
    public State getState(int q) {
        return states[q];
    }

    /**
     * Counts the number of states
     * @return the number of states
     */
    public int stateCount() {
        return states.length;
    }

    /**
     * Checks whether this DFA is complete, i.e. every state has a transition for every symbol of the alphabet
     *
     * @return true if the DFA is complete
     */
    public boolean isComplete() {
        return Arrays.stream(states).allMatch(State::isComplete);
    }
}
