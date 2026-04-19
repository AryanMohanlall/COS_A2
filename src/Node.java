public interface Node {
    double evaluate(double[] inputs);

    Node copy();

    int depth();

    int size();

    Node getNode(int index);

    Node replaceNode(int index, Node replacement);

    String toExpression();
}
