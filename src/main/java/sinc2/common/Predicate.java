package sinc2.common;

import sinc2.kb.SimpleKb;
import sinc2.util.kb.NumerationMap;

import java.util.Arrays;
import java.util.Objects;

/**
 * The class for predicates. The functor and the arguments in a predicate are represented by the numerations mapped to
 * the names.
 *
 * @since 1.0
 */
public class Predicate {
    public final int predSymbol;
    public final int[] args;

    /**
     * Initialize by the functor and the arguments specifically.
     */
    public Predicate(int predSymbol, int[] args) {
        this.predSymbol = predSymbol;
        this.args = args;
    }

    /**
     * Initialize by the functor and empty arguments (indicated by the arity).
     *
     * @param arity The arity of the predicate
     */
    public Predicate(int predSymbol, int arity) {
        this.predSymbol = predSymbol;
        this.args = new int[arity];
    }

    /**
     * A copy constructor.
     */
    public Predicate(Predicate another) {
        this.predSymbol = another.predSymbol;
        this.args = another.args.clone();
    }

    public int arity() {
        return args.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return predSymbol == predicate.predSymbol && Arrays.equals(args, predicate.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(predSymbol);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(predSymbol).append('(');
        if (0 < args.length) {
            if (Argument.isEmpty(args[0])) {
                builder.append('?');
            } else if (Argument.isVariable(args[0])) {
                builder.append('X').append(Argument.decode(args[0]));
            } else {
                builder.append(Argument.decode(args[0]));
            }
            for (int i = 1; i < args.length; i++) {
                builder.append(',');
                if (Argument.isEmpty(args[i])) {
                    builder.append('?');
                } else if (Argument.isVariable(args[i])) {
                    builder.append('X').append(Argument.decode(args[i]));
                } else {
                    builder.append(Argument.decode(args[i]));
                }
            }
        }
        builder.append(')');
        return builder.toString();
    }

    /**
     * Stringify the predicate to human-readable format with a numeration map.
     */
    public String toString(NumerationMap map) {
        StringBuilder builder = new StringBuilder(map.num2Name(predSymbol));
        builder.append('(');
        if (0 < args.length) {
            if (Argument.isEmpty(args[0])) {
                builder.append('?');
            } else if (Argument.isVariable(args[0])) {
                builder.append('X').append(Argument.decode(args[0]));
            } else {
                builder.append(map.num2Name(Argument.decode(args[0])));
            }
            for (int i = 1; i < args.length; i++) {
                builder.append(',');
                if (Argument.isEmpty(args[i])) {
                    builder.append('?');
                } else if (Argument.isVariable(args[i])) {
                    builder.append('X').append(Argument.decode(args[i]));
                } else {
                    builder.append(map.num2Name(Argument.decode(args[i])));
                }
            }
        }
        builder.append(')');
        return builder.toString();
    }

    /**
     * Stringify the predicate to human-readable format with a SimpleKb. Only the predicate symbol is translated to the
     * name.
     */
    public String toString(SimpleKb kb) {
        StringBuilder builder = new StringBuilder(kb.getRelation(predSymbol).name);
        builder.append('(');
        if (0 < args.length) {
            if (Argument.isEmpty(args[0])) {
                builder.append('?');
            } else if (Argument.isVariable(args[0])) {
                builder.append('X').append(Argument.decode(args[0]));
            } else {
                builder.append(Argument.decode(args[0]));
            }
            for (int i = 1; i < args.length; i++) {
                builder.append(',');
                if (Argument.isEmpty(args[i])) {
                    builder.append('?');
                } else if (Argument.isVariable(args[i])) {
                    builder.append('X').append(Argument.decode(args[i]));
                } else {
                    builder.append(Argument.decode(args[i]));
                }
            }
        }
        builder.append(')');
        return builder.toString();
    }
}
