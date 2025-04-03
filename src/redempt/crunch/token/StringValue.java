package redempt.crunch.token;

public class StringValue implements Value {
    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public TokenType getType() {
        return TokenType.STRING;
    }

    @Override
    public double getValue(double[] variableValues) {
        return 0;
    }

    @Override
    public Value getClone() {
       return this;
    }
}
