package redempt.crunch.functional;

import redempt.crunch.token.Token;
import redempt.crunch.token.TokenType;
import redempt.crunch.token.Value;

public interface FunctionFactory extends Token {
    @Override
    default TokenType getType() {
        return TokenType.FUNCTION_FACTORY;
    }

    Value create(ArgumentList args);
}
