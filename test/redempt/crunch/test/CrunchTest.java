package redempt.crunch.test;

import org.junit.jupiter.api.Test;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.exceptions.ExpressionCompilationException;
import redempt.crunch.exceptions.ExpressionEvaluationException;
import redempt.crunch.functional.ArgumentList;
import redempt.crunch.functional.ExpressionEnv;
import redempt.crunch.functional.FunctionFactory;
import redempt.crunch.token.LazyVariable;
import redempt.crunch.token.Value;
import redempt.crunch.token.ValueDoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

class CrunchTest {
    private static final double DELTA = 1e-7;

    @Test
    void nullTest() {
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression(null), "Null single argument");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression(null, null), "Null multi-argument");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("1", null), "Second argument null");
    }

    @Test
    void constantTest() {
        assertEquals(Math.PI, Crunch.evaluateExpression("pi"), DELTA, "Pi equality");
        assertEquals(Math.E, Crunch.evaluateExpression("e"), DELTA, "Euler's constant equality");
        assertEquals(1, Crunch.evaluateExpression("true"), DELTA, "True equal to 1");
        assertEquals(0, Crunch.evaluateExpression("false"), DELTA, "False equal to 0");
        assertEquals(-1, Crunch.evaluateExpression("-1"), "Negation operator");
    }

    @Test
    void basicOperationTest() {
        assertEquals(2, Crunch.evaluateExpression("1+1"), "Simple addition");
        assertEquals(2, Crunch.evaluateExpression("1 + 1"), "Simple expression with whitespace");
        assertEquals(2, Crunch.evaluateExpression("            1      +       1       "), "Lots of whitespace");
        assertEquals(8, Crunch.evaluateExpression("2^3"), "Simple exponent test");
        assertEquals(10, Crunch.evaluateExpression("15 - 5"), "Simple subtraction test");
        assertEquals(2, Crunch.evaluateExpression("1--1"), "Subtraction and negate operator");
        assertEquals(2, Crunch.evaluateExpression("    1     --    1"), "Somewhat confusing whitespace");
        assertEquals(5, Crunch.evaluateExpression("10 / 2"), "Asymmetric operator");
    }

    @Test
    void complexOperationTest() {
        assertEquals(9, Crunch.evaluateExpression("6/2*(1+2)"), "Order of operations");
        assertEquals(5, Crunch.evaluateExpression("6/2*1+2"), "Order of operations 2");
        assertEquals(1, Crunch.evaluateExpression("tan(atan(cos(acos(sin(asin(1))))))"), DELTA, "Trig functions");
        assertEquals(402193.3186140596, Crunch.evaluateExpression("6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4 + 6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4 + 6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4 + 6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4"), DELTA, "Large expression");
        assertEquals(-5, Crunch.evaluateExpression("1-(2)*3"), DELTA, "Weird syntax");
        assertEquals(1, Crunch.evaluateExpression("--1"), "Adjacent operators");
    }

    @Test
    void booleanLogicTest() {
        assertEquals(1, Crunch.evaluateExpression("true & true"), "Boolean and");
        assertEquals(1, Crunch.evaluateExpression("true | false"), "Boolean or");
        assertEquals(0, Crunch.evaluateExpression("true & (true & false | false)"), "More complex boolean expression");
        assertEquals(1, Crunch.evaluateExpression("1 = 1 & 3 = 3"), "Arithmetic comparisons");
        assertEquals(1, Crunch.evaluateExpression("1 != 2 & 3 != 4"), "Using !=");
    }

    @Test
    void syntaxTest() {
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("("), "Lone opening paren");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression(")"), "Lone closing paren");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("1 1"), "No operator");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("+"), "Only operator");
    }

    @Test
    void variableTest() {
        assertEquals(10, Crunch.evaluateExpression("$1", 10), "Basic variable value");
        assertEquals(14, Crunch.evaluateExpression("$1 - $2", 10, -4), "Multiple variables");
        assertThrows(ExpressionEvaluationException.class, () -> Crunch.evaluateExpression("$1"), "No variable value");

        final ExpressionEnv env = new ExpressionEnv();
        env.setVariableNames("x", "y");
        assertEquals(33, Crunch.compileExpression("x * y", env).evaluate(11, 3), "Multiplying named variables");
        assertThrows(ExpressionEvaluationException.class, () -> Crunch.compileExpression("x * y", env).evaluate(1), "Too few values");
        assertThrows(ExpressionEvaluationException.class, () -> Crunch.compileExpression("x", env).evaluate());
    }

    @Test
    void functionTest() {
        final ExpressionEnv env = new ExpressionEnv();
        env.addFunction("mult", 2, d -> d[0] * d[1]);
        env.addFunction("four", 0, d -> 4d);
        assertEquals(45, Crunch.compileExpression("mult(15, 3)", env).evaluate(), "Basic function");
        assertEquals(96, Crunch.compileExpression("mult(2, mult(4, mult(3, 4)))", env).evaluate(), "Nested functions");
        assertEquals(4, Crunch.compileExpression("four()", env).evaluate(), "No-argument function");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("mult", env), "No argument list");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("mult(1)", env), "Not enough arguments");
        assertThrows(ExpressionCompilationException.class, () -> Crunch.compileExpression("mult(1, 2, 3)", env), "Too many arguments");
    }

    @Test
    void rootingTest() {
        assertEquals(2, Crunch.evaluateExpression("sqrt(4)"), "Square Rooting");
        assertEquals(2, Crunch.evaluateExpression("cbrt(8)"), "Cube Rooting");
    }

    @Test
    void lazyVariableTest() {
        final ExpressionEnv env = new ExpressionEnv();
        env.addLazyVariable("x", () -> 2);
        env.addLazyVariable("y", () -> 7);
        assertEquals(14, Crunch.compileExpression("x*y", env).evaluate());
        assertEquals(3, Crunch.compileExpression("x + 1", env).evaluate());
    }

    @Test
    void scientificNotationTest() {
        assertEquals(2E7, Crunch.evaluateExpression("2E7"), DELTA);
    }

    @Test
    void noInlineRandomTest() {
        final CompiledExpression expr = Crunch.compileExpression("rand1000000");
        assertNotEquals(expr.evaluate(), expr.evaluate());
    }

    @Test
    void inlineTest() {
        assertEquals("6.0", Crunch.compileExpression("1 + 2 + 3").toString());
        assertEquals("-1.0", Crunch.compileExpression("-1").toString());
        assertEquals("1.0", Crunch.compileExpression("--1").toString());
    }

    @Test
    void largeExpressionWithCustomFunctionTest() {
        final ExpressionEnv env = new ExpressionEnv();
        env.addFunction("max", 2, d -> Math.max(d[0], d[1]));
        final String expr = "max( 0.0, (378044 * 100 / 100.0 - 294964) * 1.0 ) - 0.0";
        final CompiledExpression compiled = Crunch.compileExpression(expr, env);
        assertEquals(83080, compiled.evaluate());
    }

    @Test
    void cloneTest() {
        final CompiledExpression expr = Crunch.compileExpression("$1");
        assertEquals(1, expr.evaluate(1));
        assertEquals(2, expr.clone().evaluate(2));
    }

    @Test
    void functionFactoryTest() {
        final ExpressionEnv expressionEnv = new ExpressionEnv();
        expressionEnv.addFunctionFactory("function", new FunctionFactory() {
            @Override
            public Value create(ArgumentList args) {
                return (ValueDoubleSupplier) (values) -> args.getArguments()[1].getValue(values) + 1;
            }
        });
        expressionEnv.addLazyVariable("x", () -> 1);

        final CompiledExpression compiledExpression = Crunch.compileExpression("function('mult', x)", expressionEnv);
        final double evaluate = compiledExpression.evaluate();
        assertEquals(2, evaluate);
    }
}
