public class Terminal implements Node {
    private final double value;

    public Terminal(double value) {
        this.value = value;
    }

    @Override
    public double evaluate(double[] inputs) {
        return value;
    }

    @Override
    public Node copy() {
        return new Terminal(value);
    }

    @Override
    public int depth() {
        return 1;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Node getNode(int index) {
        if (index == 0) {
            return this;
        }
        throw new IndexOutOfBoundsException("Invalid node index: " + index);
    }

    @Override
    public Node replaceNode(int index, Node replacement) {
        if (index == 0) {
            return replacement.copy();
        }
        throw new IndexOutOfBoundsException("Invalid node index: " + index);
    }

    @Override
    public String toExpression() {
        return String.format("%.5f", value);
    }

    @Override
    public String toString() {
        return toExpression();
    }
}
