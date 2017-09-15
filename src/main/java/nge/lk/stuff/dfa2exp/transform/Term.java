package nge.lk.stuff.dfa2exp.transform;

/**
 * Represents a regular expression term (string prefix + optional state variable)
 */
public class Term {

    /**
     * The regular expression prefix of this term
     */
    private final String prefix;

    /**
     * The state variable. -1 if no state variable is present in this term
     */
    private final int stateVariable;

    /**
     * Creates a term which represents the empty string
     */
    public Term() {
        prefix = "";
        stateVariable = -1;
    }

    /**
     * Creates a term from a transition
     *
     * @param symbol the symbol of the transition
     * @param successor the resulting state of the transition
     */
    public Term(char symbol, int successor) {
        prefix = String.valueOf(symbol);
        stateVariable = successor;
    }

    /**
     * Creates a term
     *
     * @param prefix the regular expression prefix
     * @param stateVariable the state variable, or -1 if there is none
     */
    public Term(String prefix, int stateVariable) {
        this.prefix = prefix;
        this.stateVariable = stateVariable;
    }

    /**
     * @return the prefix of this term
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the state variable of this term, or -1 if there is no state variable present in this term
     */
    public int getStateVariable() {
        return stateVariable;
    }
}
