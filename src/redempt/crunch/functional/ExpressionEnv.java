package redempt.crunch.functional;

import redempt.crunch.Variable;
import redempt.crunch.data.CharTree;
import redempt.crunch.token.*;

import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;

/**
 * Represents an environment containing functions that can be called in expressions
 *
 * @author Redempt
 */
public class ExpressionEnv {
    private final CharTree<BinaryOperator> binaryOperators = new CharTree<>();
    private final CharTree<Token> leadingOperators = new CharTree<>();
    private final CharTree<Value> values = new CharTree<>();
    private int varCount = 0;

    /**
     * Creates a new EvaluationEnvironment
     */
    public ExpressionEnv() {
        for (final BinaryOperator operator : BinaryOperator.values()) {
            this.binaryOperators.set(operator.getSymbol(), operator);
        }
        for (final UnaryOperator operator : UnaryOperator.values()) {
            this.leadingOperators.set(operator.getSymbol(), operator);
        }
        for (final Constant constant : Constant.values()) {
            this.values.set(constant.toString().toLowerCase(Locale.ROOT), constant);
        }
    }

    public void addFunctionFactory(String name, FunctionFactory factory) {
        this.leadingOperators.set(name.toLowerCase(Locale.ROOT), factory);
    }

    /**
     * Adds a Function that can be called from expressions with this environment
     *
     * @param function The function
     */
    public ExpressionEnv addFunction(Function function) {
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null");
        }

        final String name = function.getName();
        this.checkName(name);
        this.leadingOperators.set(name, function);
        return this;
    }

    /**
     * Adds any number of Functions that can be called from expressions with this environment
     *
     * @param functions The functions to add
     */
    public ExpressionEnv addFunctions(Function... functions) {
        if (functions == null) {
            throw new IllegalArgumentException("Functions cannot be null");
        }

        for (final Function function : functions) {
            this.addFunction(function);
        }
        return this;
    }

    /**
     * Adds a lazily-evaluated variable that will not need to be passed with the variable values
     *
     * @param name   The name of the lazy variable
     * @param supply A function to supply the value of the variable when needed
     */
    public ExpressionEnv addLazyVariable(String name, DoubleSupplier supply) {
        if (supply == null) {
            throw new IllegalArgumentException("Supply cannot be null");
        }

        this.checkName(name);
        this.values.set(name, new LazyVariable(name, supply));
        return this;
    }

    public ExpressionEnv setVariableNames(String... names) {
        if (names == null) {
            throw new IllegalArgumentException("Names cannot be null");
        }

        this.varCount = names.length;
        for (int i = 0; i < names.length; i++) {
            this.checkName(names[i]);
            this.values.set(names[i], new Variable(i));
        }
        return this;
    }

    /**
     * Adds a Function that can be called from expressions with this environment
     *
     * @param name     The function name
     * @param argCount The argument count for the function
     * @param func     The lambda to accept the arguments as a double array and return a value
     */
    public ExpressionEnv addFunction(String name, int argCount, ToDoubleFunction<double[]> func) {
        this.addFunction(new Function(name, argCount, func));
        return this;
    }

    /**
     * @return The prefix tree of all leading operators, including unary operators and functions
     */
    public CharTree<Token> getLeadingOperators() {
        return this.leadingOperators;
    }

    /**
     * @return The prefix tree of all binary operators
     */
    public CharTree<BinaryOperator> getBinaryOperators() {
        return this.binaryOperators;
    }

    /**
     * @return The prefix tree of all values, including constants, variables, and lazy variables
     */
    public CharTree<Value> getValues() {
        return this.values;
    }

    /**
     * @return The number of variables in this expression environment
     */
    public int getVariableCount() {
        return this.varCount;
    }

    private void checkName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty or null");
        }
        if (!Character.isAlphabetic(name.charAt(0))) {
            throw new IllegalArgumentException("Identifier must begin with an alphabetic character");
        }
    }

}
