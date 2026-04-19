import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Program {
    public static final int MIN_NODES = 3;

    private static final double GROW_TERMINAL_PROBABILITY = 0.30;
    private static final double VARIABLE_PROBABILITY = 0.75;
    private static final double MIN_CONSTANT = -1.0;
    private static final double MAX_CONSTANT = 1.0;

    private final Node root;
    private Double fitness;

    public Program(Node root) {
        validateRoot(root);
        this.root = root;
    }

    public static List<Program> rampedHalfAndHalf(
            Random random,
            int populationSize,
            int minDepth,
            int maxDepth,
            int variableCount) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive.");
        }
        if (minDepth < 1 || maxDepth < minDepth) {
            throw new IllegalArgumentException("Invalid depth range.");
        }
        if (variableCount <= 0) {
            throw new IllegalArgumentException("Variable count must be positive.");
        }

        List<Program> programs = new ArrayList<>();
        int depthRange = maxDepth - minDepth + 1;

        for (int i = 0; i < populationSize; i++) {
            int depth = minDepth + (i % depthRange);
            boolean full = i % 2 == 0;
            programs.add(randomProgram(random, depth, variableCount, full));
        }

        return programs;
    }

    public static Program randomProgram(Random random, int maxDepth, int variableCount, boolean full) {
        return new Program(randomFunctionTree(random, maxDepth, variableCount, full));
    }

    public static Program growProgram(Random random, int maxDepth, int variableCount) {
        return randomProgram(random, maxDepth, variableCount, false);
    }

    public double evaluate(double[] inputs) {
        return root.evaluate(inputs);
    }

    public Program copy() {
        Program copy = new Program(root.copy());
        copy.setFitness(fitness);
        return copy;
    }

    public Program deepCopy() {
        return copy();
    }

    public Double getFitness() {
        return fitness;
    }

    public void setFitness(Double fitness) {
        this.fitness = fitness;
    }

    public int depth() {
        return root.depth();
    }

    public int size() {
        return root.size();
    }

    public String toExpression() {
        return root.toExpression();
    }

    public Node getRoot() {
        return root;
    }

    public boolean isValid() {
        return isValidRoot(root);
    }

    private static void validateRoot(Node root) {
        if (!isValidRoot(root)) {
            throw new IllegalArgumentException("Program root must be a function node with at least "
                    + MIN_NODES + " nodes.");
        }
    }

    private static boolean isValidRoot(Node root) {
        return root instanceof Function && root.size() >= MIN_NODES;
    }

    private static Node randomFunctionTree(Random random, int maxDepth, int variableCount, boolean full) {
        return new Function(
                randomOperator(random),
                randomTree(random, maxDepth - 1, variableCount, full),
                randomTree(random, maxDepth - 1, variableCount, full));
    }

    private static Node randomTree(Random random, int maxDepth, int variableCount, boolean full) {
        if (maxDepth <= 1 || (!full && random.nextDouble() < GROW_TERMINAL_PROBABILITY)) {
            return randomTerminal(random, variableCount);
        }

        return new Function(
                randomOperator(random),
                randomTree(random, maxDepth - 1, variableCount, full),
                randomTree(random, maxDepth - 1, variableCount, full));
    }

    private static double randomConstant(Random random) {
        return MIN_CONSTANT + random.nextDouble() * (MAX_CONSTANT - MIN_CONSTANT);
    }

    public static Node randomTerminal(Random random, int variableCount) {
        if (random.nextDouble() < VARIABLE_PROBABILITY) {
            return new Variable(random.nextInt(variableCount));
        }

        return new Terminal(randomConstant(random));
    }

    public static Function.Operator randomOperator(Random random) {
        Function.Operator[] operators = Function.Operator.values();
        return operators[random.nextInt(operators.length)];
    }

    public int depthAt(int nodeIndex) {
        return depthAt(root, nodeIndex, 1, new int[] { 0 });
    }

    public boolean isFunctionAt(int nodeIndex) {
        return getRoot().getNode(nodeIndex) instanceof Function;
    }

    private int depthAt(Node node, int targetIndex, int depth, int[] currentIndex) {
        if (currentIndex[0] == targetIndex) {
            return depth;
        }

        if (node instanceof Function) {
            Function function = (Function) node;
            currentIndex[0]++;
            int leftDepth = depthAt(function.getLeft(), targetIndex, depth + 1, currentIndex);
            if (leftDepth != -1) {
                return leftDepth;
            }

            currentIndex[0]++;
            return depthAt(function.getRight(), targetIndex, depth + 1, currentIndex);
        }

        return -1;
    }

    @Override
    public String toString() {
        return toExpression();
    }
}
