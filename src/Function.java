public class Function implements Node {
    public enum Operator {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MIN("min"),
        IF_LESS_THAN_ZERO("<0");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private static final double DIVISION_EPSILON = 1.0e-9;

    private final Operator operator;
    private final Node left;
    private final Node right;

    public Function(Operator operator, Node left, Node right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    @Override
    public double evaluate(double[] inputs) {
        double leftValue = left.evaluate(inputs);
        double rightValue = right.evaluate(inputs);

        switch (operator) {
            case ADD:
                return leftValue + rightValue;
            case SUBTRACT:
                return leftValue - rightValue;
            case MULTIPLY:
                return leftValue * rightValue;
            case DIVIDE:
                if (Math.abs(rightValue) < DIVISION_EPSILON) {
                    return 1.0;
                }
                return leftValue / rightValue;
            case MIN:
                return Math.min(leftValue, rightValue);
            case IF_LESS_THAN_ZERO:
                return leftValue < 0.0 ? rightValue : 0.0;
            default:
                throw new IllegalStateException("Unknown operator: " + operator);
        }
    }

    @Override
    public Node copy() {
        return new Function(operator, left.copy(), right.copy());
    }

    @Override
    public int depth() {
        return 1 + Math.max(left.depth(), right.depth());
    }

    @Override
    public int size() {
        return 1 + left.size() + right.size();
    }

    @Override
    public Node getNode(int index) {
        if (index == 0) {
            return this;
        }

        int leftSize = left.size();
        if (index <= leftSize) {
            return left.getNode(index - 1);
        }

        return right.getNode(index - 1 - leftSize);
    }

    @Override
    public Node replaceNode(int index, Node replacement) {
        if (index == 0) {
            return replacement.copy();
        }

        int leftSize = left.size();
        if (index <= leftSize) {
            return new Function(operator, left.replaceNode(index - 1, replacement), right.copy());
        }

        return new Function(operator, left.copy(), right.replaceNode(index - 1 - leftSize, replacement));
    }

    @Override
    public String toExpression() {
        if (operator == Operator.MIN) {
            return "min(" + left.toExpression() + ", " + right.toExpression() + ")";
        }
        if (operator == Operator.IF_LESS_THAN_ZERO) {
            return "if(" + left.toExpression() + " < 0, " + right.toExpression() + ", 0)";
        }
        return "(" + left.toExpression() + " " + operator.getSymbol() + " " + right.toExpression() + ")";
    }

    @Override
    public String toString() {
        return toExpression();
    }
}
