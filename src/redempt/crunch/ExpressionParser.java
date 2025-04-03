package redempt.crunch;

import redempt.crunch.data.FastNumberParsing;
import redempt.crunch.exceptions.ExpressionCompilationException;
import redempt.crunch.functional.*;
import redempt.crunch.token.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExpressionParser {

    private final String input;
    private final ExpressionEnv environment;
    private final CompiledExpression expression = new CompiledExpression();

    private int maxVarIndex;
    private int cursor = 0;

    ExpressionParser(String input, ExpressionEnv env) {
        if (input == null) {
            throw new ExpressionCompilationException(null, "Expression is null");
        }
        if (env == null) {
            throw new ExpressionCompilationException(null, "Environment is null");
        }
        maxVarIndex = env.getVariableCount() - 1;
        this.input = input.replace(" ", "");
        this.environment = env;
    }

    public char peek() {
        return input.charAt(cursor);
    }

    public char advance() {
        return input.charAt(cursor++);
    }

    public void advanceCursor() {
        cursor++;
    }

    public boolean isAtEnd() {
        return cursor >= input.length();
    }

    public int getCursor() {
        return cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public String getInput() {
        return input;
    }

    public void expectChar(char c) {
        if (isAtEnd() || advance() != c) {
            throw new ExpressionCompilationException(this, "Expected '" + c + "'");
        }
    }

    private void error(String msg) {
        throw new ExpressionCompilationException(this, msg);
    }

    private boolean whitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            cursor++;
        }
        return true;
    }

    private Value parseExpression() {
        if (isAtEnd()) {
            error("Expected expression");
        }
        Value first = parseTerm();
        if (isAtEnd() || peek() == ')' || peek() == ',') {
            return first;
        }
        ShuntingYard tokens = new ShuntingYard();
        tokens.addValue(first);
        while (whitespace() && !isAtEnd() && peek() != ')' && peek() != ',') {
            BinaryOperator token = environment.getBinaryOperators().getWith(this);
            if (token == null) {
                error("Expected binary operator");
            }
            tokens.addOperator(token);
            whitespace();
            tokens.addValue(parseTerm());
        }
        return tokens.finish();
    }

    private Value parseNestedExpression() {
        expectChar('(');
        whitespace();
        Value expression = parseExpression();
        expectChar(')');
        return expression;
    }

    private Value parseAnonymousVariable() {
        expectChar('$');
        double value = parseLiteral().getValue(new double[0]);
        if (value % 1 != 0) {
            error("Decimal variable indices are not allowed");
        }
        if (value < 1) {
            error("Zero and negative variable indices are not allowed");
        }
        int index = (int) value - 1;
        maxVarIndex = Math.max(index, maxVarIndex);
        return new Variable(index);
    }

    private Value parseTerm() {
        switch (peek()) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                return parseLiteral();
            case '(':
                return parseNestedExpression();
            case '$':
                return parseAnonymousVariable();
            case '\'':
                return parseString();
            default:
                break; // Ignore
        }

        Token leadingOperator = environment.getLeadingOperators().getWith(this);
        if (leadingOperator != null) {
            return parseLeadingOperation(leadingOperator);
        }
        Value term = environment.getValues().getWith(this);
        if (term == null) {
            error("Expected value");
        }
        return term;
    }

    private StringValue parseString() {
        // skip \"
        advanceCursor();

        // parse everything till we find another "
        int start = cursor;

        while ('\'' != peek()) {
            advanceCursor();
            if (isAtEnd()) {
                break;
            }
        }
        expectChar('\'');

        return new StringValue(input.substring(start, cursor));
    }

    private LiteralValue parseLiteral() {
        int start = cursor;
        char c;
        while (Character.isDigit(c = peek()) || c == '.') {
            advanceCursor();
            if (isAtEnd()) {
                break;
            }
        }
        return new LiteralValue(FastNumberParsing.parseDouble(input, start, cursor));
    }

    private Value parseLeadingOperation(Token token) {
        whitespace();
        switch (token.getType()) {
            case UNARY_OPERATOR:
                UnaryOperator op = (UnaryOperator) token;
                Value term = parseTerm();
                if (op.isPure() && term.getType() == TokenType.LITERAL_VALUE) {
                    return new LiteralValue(op.getOperation().applyAsDouble(term.getValue(new double[0])));
                }
                return new UnaryOperation(op, term);
            case FUNCTION:
                Function function = (Function) token;
                ArgumentList args = parseArgumentList(function.getArgCount());
                return new FunctionCall(function, args.getArguments());

            case FUNCTION_FACTORY:
                FunctionFactory factory = (FunctionFactory) token;
                args = parseAllArguments();

                return Objects.requireNonNull(factory.create(args), String.format("Factory %s returned null function", factory));
        }
        error("Expected leading operation");
        return null;
    }

    private ArgumentList parseAllArguments() {
        expectChar('(');
        whitespace();

        final List<Value> arguments = new ArrayList<>(2);

        // Till we reach end of function
        while (peek() != ')') {
            Value value = parseExpression();
            arguments.add(value);

            if (!isAtEnd() && peek() != ')') {
                expectChar(',');
            }
        }
        expectChar(')');

        return new ArgumentList(arguments.toArray(new Value[0]));
    }

    private ArgumentList parseArgumentList(int args) {
        expectChar('(');
        whitespace();
        Value[] values = new Value[args];
        if (args == 0) {
            expectChar(')');
            return new ArgumentList(new Value[0]);
        }
        values[0] = parseExpression();
        whitespace();
        for (int i = 1; i < args; i++) {
            expectChar(',');
            whitespace();
            values[i] = parseExpression();
            whitespace();
        }

        expectChar(')');
        return new ArgumentList(values);
    }

    public CompiledExpression parse() {
        whitespace();
        Value value = parseExpression();
        whitespace();
        if (!isAtEnd()) {
            error("Dangling term");
        }
        expression.initialize(value, maxVarIndex + 1);
        return expression;
    }

}