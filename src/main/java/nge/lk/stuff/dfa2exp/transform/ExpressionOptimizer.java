package nge.lk.stuff.dfa2exp.transform;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Optimizes regular expressions created by solving {@link EquationSystem}s
 *
 * The optimizer does only work for syntactically correct expressions with some other restrictions.
 * Those restrictions are respected by the equation system solver
 */
public class ExpressionOptimizer {

    /**
     * The expression that is being optimized
     */
    private final String expression;

    /**
     * The terms that are present in the expression (on the highest level)
     */
    private final List<String> terms;

    /**
     * Whether the expression represents a disjunction (term1|term2|...) or a concatenation (term1term2...) of terms
     */
    private boolean isDisjunction;

    /**
     * Checks whether the given regular expression R is atomic, i.e. if (R)Q is equivalent to RQ for a quantifier Q
     *
     * @param expr the regular expression
     *
     * @return true if the expression is atomic
     */
    public static boolean isAtomic(String expr) {
        if (expr.length() == 1 && !"()[]|*+?".contains(expr)) {
            return true;
        }

        // Check whether the expression is in [] or () and there's only one top-level term
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

            // The top level ()/[] ended before the end of the string was reached.
            // Alternatively, the first character did not increment the depth (this also catches XQ, X character, Q quantifier, but those are treated as concatenated for this purpose)
            if (depth == 0 && i != data.length - 1) {
                return false;
            }
        }

        return depth == 0;
    }

    /**
     * Checks whether the given character is a quantifier (*, +, ?)
     *
     * @param c the character
     *
     * @return true if it is a quantifier
     */
    private static boolean isQuantifier(char c) {
        return "*+?".contains(String.valueOf(c));
    }

    /**
     * Create an optimizer for the given expression
     *
     * @param expr the expression
     */
    public ExpressionOptimizer(String expr) {
        expression = expr;
        terms = new ArrayList<>();
    }

    /**
     * Optimizes the expression
     *
     * @return the optimized expression
     */
    public String optimize() {
        // The expression is first classified as a disjunction (contains top-level |) or a concatenation (otherwise)
        classify();

        // It is then split according to the rules of its type (disjunction: terms are conjunctions, concatenation: terms are (quantified) atoms)
        split();

        // Each of the individual terms of this expression will be optimized first
        optimizeTerms();

        // Optimizes the disjunction/concatenation of the optimized terms
        optimizeExpression();

        // Recombines the terms into an expression
        return recombine();
    }

    /**
     * Recombines the terms into an expression
     *
     * @return the recombined terms
     */
    private String recombine() {
        return String.join(isDisjunction ? "|" : "", terms);
    }

    /**
     * Optimizes the disjunction/concatenation of the optimized terms
     */
    private void optimizeExpression() {
        assert !terms.isEmpty() : "Empty set of terms";

        if (isDisjunction) {
            // Extracts a common prefix/suffix from all disjunction terms.
            // Example: abcd|aefd|aghd -> a(bc|ef|gh)d
            // Special care is taken for cases like ab|acb -> a(c?)b [parenthesis might be subject to further optimization]
            extractPrefixAndSuffix();
        } else {
            if (terms.size() > 1) {
                // If there is more than one term, reduction will combine terms like R R* into R+
                reduceTerms();
            }
        }
    }

    /**
     * This optimization will combine terms like R R* into R+ in a concatenation
     */
    private void reduceTerms() {
        Deque<String> newTerms = new ArrayDeque<>();
        for (String term : terms) {
            if (newTerms.isEmpty()) {
                // Special case for the first term
                newTerms.addLast(term);
            } else {
                // Combine the previous term and the current term if possible
                String peek = newTerms.peekLast();
                if (term.length() == peek.length() && term.substring(0, term.length() - 1).equals(peek.substring(0, peek.length() - 1))) {
                    // RP RQ -> ??
                    if (term.equals(peek)) {
                        // P == Q
                        if (term.endsWith("*")) {
                            // R* R* -> R*
                            // peek is still in the deque, just don't add term
                        } else {
                            newTerms.addLast(term);
                        }
                    } else {
                        // P != Q
                        if (term.endsWith("*") && peek.endsWith("+") || term.endsWith("?") && peek.endsWith("*") || term.endsWith("?") && peek.endsWith("+")) {
                            // R+ R* -> R+
                            // peek is still in the deque, just don't add term
                        } else if (term.endsWith("+") && peek.endsWith("*") || term.endsWith("*") && peek.endsWith("?") || term.endsWith("+") && peek.endsWith("?")) {
                            // R* R+ -> R+ / R? R* -> R* / R? R+ -> R+
                            newTerms.pollLast();
                            newTerms.addLast(term);
                        } else {
                            newTerms.addLast(term);
                        }
                    }
                } else if (term.length() == peek.length() + 1 && term.endsWith("*") && term.startsWith(peek)) {
                    // R R* -> R+
                    newTerms.pollLast();
                    newTerms.addLast(peek + "+");
                } else if (term.length() + 1 == peek.length() && peek.endsWith("*") && peek.startsWith(term)) {
                    // R* R -> R+
                    newTerms.pollLast();
                    newTerms.addLast(term + "+");
                } else {
                    newTerms.addLast(term);
                }
            }
        }
        terms.clear();
        terms.addAll(newTerms);
    }

    /**
     * Finds the length of the maximum prefix of all terms
     *
     * @param minimal the smallest term (i.e. the longest possible prefix)
     *
     * @return the length of the longest possible prefix
     */
    private int findPrefixLength(String minimal) {
        int prefixLength = 0;
        while (prefixLength < minimal.length()) { // If prefixLength == minimalLength, this loop does not need to be run because this loop only increases the size
            // Invariant: minimal[0..prefixLength) is a valid prefix

            int tmpLength = prefixLength;
            if (minimal.charAt(tmpLength) == '[') {
                // The class has to be consumed in one piece. It is safe to assume that the next ] closes the class (equation systems don't allow ] in alphabets)
                tmpLength = minimal.indexOf(']', tmpLength) + 1;
            } else if (minimal.charAt(tmpLength) == '(') {
                int depth = 1;
                tmpLength++; // Consume the (
                while (depth != 0) {
                    if (minimal.charAt(tmpLength) == '(') {
                        depth++;
                    } else if (minimal.charAt(tmpLength) == ')') {
                        depth--;
                    }
                    tmpLength++; // Consume contents
                }
                // Everything to the closing ) is now consumed
            } else {
                // The next character is independent (ignoring quantifiers)
                tmpLength++;
            }

            // Check whether the previously consumed expression is quantified
            if (tmpLength < minimal.length() && isQuantifier(minimal.charAt(tmpLength))) {
                tmpLength++; // Consume the quantifier
            }

            // Check whether the expanded 'prefix' is really a prefix of all terms
            String newPrefix = minimal.substring(0, tmpLength);
            if (terms.stream().allMatch(t -> t.startsWith(newPrefix))) {
                prefixLength = tmpLength; // It is, make it the new prefix
            } else {
                break; // the prefix minimal[0..tmplength) is no valid prefix (but thanks to the invariant, minimal[0..prefixlength) is one), so we've already found the largest prefix
            }
        }

        return prefixLength;
    }

    /**
     * Finds the length of the maximum suffix of all terms that does not intersect with the prefix
     *
     * @param minimal the smallest term (for determining the longest possible suffix)
     * @param prefixLength the length of the maximum prefix (for avoiding intersection)
     *
     * @return the length of the longest possible suffix
     */
    private int findSuffixLength(String minimal, int prefixLength) {
        int suffixLength = 0;
        while (prefixLength + suffixLength < minimal.length()) { // Ensure that there is no intersection
            // Invariant: the last `suffixLength` characters of minimal are a valid suffix that does not intersect with the prefix
            // Invariant after every tmpLength/helperIndex update is: tmpLength is the length of the current suffix (valid length)
            //   and helperIndex points at the next character that could be added to the suffix (might be -1 if the suffix encompasses the whole string)
            //  (helperIndex is recalculated from scratch every iteration)

            // Suffix extension starts at the back of the suffix
            int tmpLength = suffixLength;
            int helperIndex = minimal.length() - tmpLength - 1;

            // Consume a quantifier, if there is one (quantifying the next term)
            if (isQuantifier(minimal.charAt(helperIndex))) {
                tmpLength++;
                helperIndex--;
            }

            if (minimal.charAt(helperIndex) == ']') {
                // Consume the whole class. It is safe to assume that the next [ is the corresponding [.
                int idxOpen = minimal.lastIndexOf('[', helperIndex);
                tmpLength += helperIndex - idxOpen + 1; // helperIndex - idxOpen + 1 is the length of the group
                // helperIndex = idxOpen - 1; // not needed, helperIndex is not used after this point
            } else if (minimal.charAt(helperIndex) == ')') {
                int depth = 1;
                tmpLength++; // Consume the )
                helperIndex--;
                while (depth != 0) {
                    if (minimal.charAt(helperIndex) == '(') {
                        depth--;
                    } else if (minimal.charAt(helperIndex) == ')') {
                        depth++;
                    }
                    tmpLength++; // Consume contents
                    helperIndex--;
                }
            } else {
                // Consume a single character
                tmpLength++;
                // helperIndex--; // not needed, helperIndex is not used after this point
            }

            // Calculate the new suffix and check if it is a valid suffix
            String newSuffix = minimal.substring(minimal.length() - tmpLength);
            if (newSuffix.length() + prefixLength <= minimal.length() && terms.stream().allMatch(t -> t.endsWith(newSuffix))) { // the new suffix might have grown into the prefix
                suffixLength = tmpLength;
            } else {
                break; // The new suffix is invalid, we already have the longest suffix
            }
        }

        return suffixLength;
    }

    /**
     * Extracts a common prefix and suffix from the terms (only for disjunctions)
     */
    private void extractPrefixAndSuffix() {
        String minimal = terms.stream().filter(t -> t.length() == terms.stream().mapToInt(String::length).min().getAsInt()).findAny().get();
        int prefixLength = findPrefixLength(minimal);
        int suffixLength = findSuffixLength(minimal, prefixLength);
        assert prefixLength + suffixLength <= minimal.length() : "Prefix and suffix intersect";

        // No common prefix/suffix found
        if (prefixLength == 0 && suffixLength == 0) {
            return;
        }

        String prefix = minimal.substring(0, prefixLength);
        String suffix = minimal.substring(minimal.length() - suffixLength);

        // Extract the centers (parts without prefix/suffix) from the terms
        List<String> centers = new LinkedList<>();
        boolean centersContainEmptyString = false;
        for (String term : terms) {
            if (term.length() == prefixLength + suffixLength) {
                centersContainEmptyString = true; // The term did only contain prefix & suffix. The center part must accept the empty word in this case
                continue;
            }
            String center = term.substring(prefixLength, term.length() - suffixLength);
            centers.add(center);
        }

        // Heuristic if the empty word is allowed by the centers. As the centers are a disjunction any atomic disjunction term quantified with * or ? satisfies this.
        // Things like R*Q* are not considered by the heuristic as they would require a more sophisticated parsing.
        boolean definitelyAllowsEmptyWord = centers.stream().anyMatch(c -> isAtomic(c.substring(0, c.length() - 1)) && (c.endsWith("*") || c.endsWith("?")));
        if (!definitelyAllowsEmptyWord && centersContainEmptyString) {
            // In this case, the center needs to allow the empty word. We just modify one disjunction term for that purpose.

            // To optimize the chance of finding a term that can be easily modified to allow the empty word we sort by length, ascending.
            centers.sort(Comparator.comparingInt(String::length));
            String modify = centers.remove(0);

            if (isAtomic(modify)) {
                // The term is atomic and can receive the ? quantifier
                centers.add(modify + "?");
            } else if (modify.endsWith("+") && isAtomic(modify.substring(0, modify.length() - 1))) {
                // The term is a quantified atomic term (+). Just change + to *.
                centers.add(modify.substring(0, modify.length() - 1) + "*");
            } else {
                // We're out of luck. Add a ? quantifier to the term
                centers.add("(" + modify + ")?");
            }
        }

        String centerPart = String.join("|", centers);
        if (centers.size() > 1) {
            // At this point, a prefix/suffix will be concatenated. So ignoring parenthesis for exactly ONE center is fine, as this just increases the concatenation's length
            // But in this case, there's more than one center.
            centerPart = "(" + centerPart + ")";
        }
        terms.clear();
        terms.add(prefix + centerPart + suffix);
    }

    /**
     * Optimize the individual terms of this expression
     */
    private void optimizeTerms() {
        Collection<String> optimizedTerms = new LinkedList<>();
        for (String term : terms) {
            if (isDisjunction) {
                // Disjunction terms are non-trivial, so another optimizer is started for the whole term.
                ExpressionOptimizer subProblem = new ExpressionOptimizer(term);
                optimizedTerms.add(subProblem.optimize());
            } else {
                // Concatenation terms are atomic or quantified atomic. They can receive more sophisticated optimizations on the fly.
                if (term.startsWith("(")) {
                    // Check the quantifier of the group
                    char quantifier = isQuantifier(term.charAt(term.length() - 1)) ? term.charAt(term.length() - 1) : 0;

                    // The exclusive end index of the term without quantifier and )
                    int end = term.length() - (quantifier != 0 ? 2 : 1);

                    // Create a new subproblem for the contents of the group and optimize it
                    ExpressionOptimizer subProblem = new ExpressionOptimizer(term.substring(1, end));
                    String optimized = subProblem.optimize();

                    if (!isAtomic(optimized)) {
                        // When the optimization contains top level disjunctions or has a quantifier, parenthesis are needed
                        if (quantifier != 0 || subProblem.hasDisjunctionPrecedenceProblems()) {
                            optimized = "(" + optimized + ")" + (quantifier != 0 ? quantifier : "");
                        }
                        // Otherwise, no parenthesis are needed (as this is a concatenation)
                    } else if (quantifier != 0) {
                        // Atomic, with quantifier. Just append the quantifier to the atomic term.
                        optimized += quantifier;
                    } // otherwise, optimized is atomic with no quantifier and needs no modification
                    optimizedTerms.add(optimized);
                } else {
                    // The term is either a (quantified) group or a (quantified) character
                    // This is the base case for children recursion. Every regular expression can be broken down into these (ignoring combination syntax like |() etc.)
                    optimizedTerms.add(term);
                }
            }
        }
        terms.clear();
        terms.addAll(optimizedTerms);
    }

    /**
     * @return true if the expression that was optimized is a disjunction with more than one term (creates operator precedence problems with concatenation)
     */
    private boolean hasDisjunctionPrecedenceProblems() {
        return isDisjunction && terms.size() > 1;
    }

    /**
     * Splits the expression into terms depending on the type
     */
    private void split() {
        if (isDisjunction) {
            splitDisjunction();
        } else {
            splitConcatenation();
        }
    }

    /**
     * Splits a concatenation. Concatenations are split into (quantified) atomic terms
     */
    private void splitConcatenation() {
        int i = 0;
        StringBuilder partBuilder = new StringBuilder();

        // Every iteration of this loop produces exactly one term
        while (i < expression.length()) {
            if (expression.charAt(i) == '[') {
                int endIndex = expression.indexOf(']', i);
                assert endIndex != -1 : "Unclosed class";

                // Consume the whole character class
                partBuilder.append(expression.substring(i, endIndex + 1));
                i = endIndex + 1;
            } else if (expression.charAt(i) == '(') {
                partBuilder.append('(');
                i++;

                int depth = 1;
                while (i < expression.length() && depth != 0) {
                    if (expression.charAt(i) == '(') {
                        depth++;
                    } else if (expression.charAt(i) == ')') {
                        depth--;
                    }
                    partBuilder.append(expression.charAt(i));
                    i++;
                }
                // At this point, the whole group is consumed

                assert depth == 0 : "Premature expression end";
            } else {
                // Consume a character
                partBuilder.append(expression.charAt(i++));
            }

            // The previously consumed atom is quantified
            if (i < expression.length() && isQuantifier(expression.charAt(i))) {
                // Expand the atom to the quantified atomic term
                partBuilder.append(expression.charAt(i));
                i++;
            }

            terms.add(partBuilder.toString());
            partBuilder.setLength(0);
        }
    }

    /**
     * Splits a disjunction. Disjunctions are split at top level | into concatenations (with quantified atomic terms being a special case of concatenation)
     */
    private void splitDisjunction() {
        int depth = 0;
        StringBuilder partBuilder = new StringBuilder();
        for (char c : expression.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }

            // Only recognize disjunction operators that are at the highest level, depth 0
            if (depth == 0 && c == '|') {
                // Reset the builder and add a new term
                terms.add(partBuilder.toString());
                partBuilder.setLength(0);
            } else {
                partBuilder.append(c);
            }
        }

        // Add the last term (which may not be empty, otherwise the expression was empty or the expression ends in |)
        terms.add(partBuilder.toString());
    }

    /**
     * Classifies the expression
     */
    private void classify() {
        int depth = 0;
        isDisjunction = false;
        for (char c : expression.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }

            // A disjunction operator at depth 0 is the only way to make something a disjunction
            if (depth == 0 && c == '|') {
                isDisjunction = true;
                break;
            }
        }
    }
}
