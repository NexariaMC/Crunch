package redempt.crunch.token;

@FunctionalInterface
public interface ValueDoubleSupplier extends Value {
    @Override
    default Value getClone() {
        return this;
    }

    @Override
    default TokenType getType() {
        return TokenType.FUNCTION;
    }
}
