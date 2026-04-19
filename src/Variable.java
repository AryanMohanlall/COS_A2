public class Variable implements Node {
    private static String[] names = new String[0];

    private final int index;

    public Variable(int index) {
        this.index = index;
    }

    public static void setNames(String[] variableNames) {
        names = variableNames.clone();
    }

    @Override
    public double evaluate(double[] inputs) {
        if (index < 0 || index >= inputs.length) {
            throw new IllegalArgumentException("Input " + getName() + " is not available.");
        }
        return inputs[index];
    }

    @Override
    public Node copy() {
        return new Variable(index);
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
    public Node getNode(int nodeIndex) {
        if (nodeIndex == 0) {
            return this;
        }
        throw new IndexOutOfBoundsException("Invalid node index: " + nodeIndex);
    }

    @Override
    public Node replaceNode(int nodeIndex, Node replacement) {
        if (nodeIndex == 0) {
            return replacement.copy();
        }
        throw new IndexOutOfBoundsException("Invalid node index: " + nodeIndex);
    }

    @Override
    public String toExpression() {
        return getName();
    }

    @Override
    public String toString() {
        return toExpression();
    }

    private String getName() {
        if (index >= 0 && index < names.length) {
            return names[index];
        }
        return "x" + index;
    }
}
