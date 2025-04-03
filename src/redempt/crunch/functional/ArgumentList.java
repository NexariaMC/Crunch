package redempt.crunch.functional;

import redempt.crunch.token.Token;
import redempt.crunch.token.TokenType;
import redempt.crunch.token.Value;

import java.util.Arrays;

/**
 * Represents a list of arguments being passed to a Function
 *
 * @author Redempt
 */
public class ArgumentList implements Token {
    private final Value[] arguments;

    public ArgumentList(Value[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public TokenType getType() {
        return TokenType.ARGUMENT_LIST;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.arguments);
    }

    public Value[] getArguments() {
        return this.arguments;
    }
}
