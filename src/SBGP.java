import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SBGP {
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";

    public Program[] population;
    public int population_size;
    public Double crossover_rate;
    public Double mutation_rate;
    public Double hoist_rate;
    public Dataset dataset;
    public int max_depth;
    public int min_depth;
    public int generations;
    public Double bloat_penalty;

    private static final int GLOBAL_SEARCH_GENERATIONS = 10;
    private static final int GLOBAL_AREA_DEPTH = 4;
    private static final int GLOBAL_SIMILARITY_THRESHOLD = 6;
    private static final int LOCAL_WINDOW_TOLERANCE = 10;

    private final long seed;
    private final Random random;
    private final int tournamentSize;
    private final int mutationDepth;
    private final Program[] exploitedGlobalAreas;
    private long startTime;
    private long endTime;
    private int variableCount;
    private double trainingRmse;
    private double validationRmse;
    private int exploitedGlobalAreaCount;
    private boolean localSearchActive;

    public SBGP(
            int populationSize,
            Double crossoverRate,
            Double mutationRate,
            Double hoistRate,
            Dataset dataset,
            int minDepth,
            int maxDepth,
            int generations) {
        this(
                populationSize,
                crossoverRate,
                mutationRate,
                hoistRate,
                dataset,
                minDepth,
                maxDepth,
                generations,
                System.currentTimeMillis());
    }

    public SBGP(
            int populationSize,
            Double crossoverRate,
            Double mutationRate,
            Double hoistRate,
            Dataset dataset,
            int minDepth,
            int maxDepth,
            int generations,
            long seed) {
        if (crossoverRate + mutationRate + hoistRate > 1.0) {
            throw new IllegalArgumentException("Rates must sum to <= 1.0.");
        }
        if (populationSize <= 0 || generations < 0) {
            throw new IllegalArgumentException("Population size must be positive and generations cannot be negative.");
        }
        if (minDepth < 1 || maxDepth < minDepth) {
            throw new IllegalArgumentException("Invalid depth range.");
        }
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset must not be null.");
        }

        this.population_size = populationSize;
        this.population = new Program[this.population_size];
        this.crossover_rate = crossoverRate;
        this.mutation_rate = mutationRate;
        this.hoist_rate = hoistRate;
        this.bloat_penalty = 0.00001;
        this.dataset = dataset;
        this.min_depth = minDepth;
        this.max_depth = maxDepth;
        this.generations = generations;
        this.seed = seed;
        this.random = new Random(seed);
        this.tournamentSize = 4;
        this.mutationDepth = 3;
        this.exploitedGlobalAreas = new Program[this.generations + 1];
        this.variableCount = dataset.getVariableCount();
    }

    public SBGP(Random random) {
        this.random = random;
        this.seed = -1;
        this.population_size = 120;
        this.population = new Program[this.population_size];
        this.crossover_rate = 0.80;
        this.mutation_rate = 0.15;
        this.hoist_rate = 0.0;
        this.bloat_penalty = 0.00001;
        this.min_depth = 2;
        this.max_depth = 4;
        this.generations = 60;
        this.tournamentSize = 4;
        this.mutationDepth = 3;
        this.exploitedGlobalAreas = new Program[this.generations + 1];
    }

    public void train() {
        ensureDatasetAvailable();

        this.startTime = System.currentTimeMillis();
        this.variableCount = dataset.getVariableCount();

        initialPopulation();
        evaluatePopulation(dataset.getTrainingInputs(), dataset.getTrainingTargets());
        sortPopulation();

        structureBasedSearch(dataset.getTrainingInputs(), dataset.getTrainingTargets());

        this.trainingRmse = rmse(population[0], dataset.getTrainingInputs(), dataset.getTrainingTargets());
        this.validationRmse = rmse(population[0], dataset.getValidationInputs(), dataset.getValidationTargets());
        this.endTime = System.currentTimeMillis();
    }

    public Program evolve(
            double[][] trainingInputs,
            double[] trainingTargets,
            double[][] validationInputs,
            double[] validationTargets,
            int variableCount) {
        validateData(trainingInputs, trainingTargets, variableCount);
        validateData(validationInputs, validationTargets, variableCount);

        this.startTime = System.currentTimeMillis();
        this.variableCount = variableCount;
        Variable.setNames(defaultVariableNames(variableCount));

        initialPopulation();
        evaluatePopulation(trainingInputs, trainingTargets);
        sortPopulation();

        structureBasedSearch(trainingInputs, trainingTargets);

        this.trainingRmse = rmse(population[0], trainingInputs, trainingTargets);
        this.validationRmse = rmse(population[0], validationInputs, validationTargets);
        this.endTime = System.currentTimeMillis();
        return population[0].deepCopy();
    }

    public Double test() {
        ensureDatasetAvailable();
        ensureTrained();
        return rmse(population[0], dataset.getTestingInputs(), dataset.getTestingTargets());
    }

    public void print() {
        ensureTrained();

        System.out.println();
        System.out.println(GREEN + "|==============SBGP RUN SUMMARY=============|" + RESET);

        System.out.println();
        System.out.println("  Train RMSE: " + trainingRmse);
        if (dataset != null) {
            System.out.println("  Test RMSE:  " + test());
        } else {
            System.out.println("  Validation RMSE: " + validationRmse);
        }

        System.out.println(YELLOW + "\n  -- Parameters ----------------------------" + RESET);
        System.out.println(YELLOW + "  Population size   : " + RESET + this.population_size);
        System.out.println(YELLOW + "  Generations       : " + RESET + this.generations);
        System.out.println(YELLOW + "  Min / Max depth   : " + RESET + this.min_depth + " / " + this.max_depth);
        System.out.println(YELLOW + "  Crossover rate    : " + RESET + this.crossover_rate);
        System.out.println(YELLOW + "  Mutation rate     : " + RESET + this.mutation_rate);
        System.out.println(YELLOW + "  Hoist rate        : " + RESET + this.hoist_rate);
        System.out.println(YELLOW + "  Bloat penalty     : " + RESET + this.bloat_penalty);
        System.out.println(YELLOW + "  Global gens (Sg)  : " + RESET + GLOBAL_SEARCH_GENERATIONS);
        System.out.println(YELLOW + "  Global depth (Dg) : " + RESET + GLOBAL_AREA_DEPTH);
        System.out.println(YELLOW + "  Similarity (Tg)   : " + RESET + GLOBAL_SIMILARITY_THRESHOLD);
        System.out.println(YELLOW + "  Local window (Wg) : " + RESET + LOCAL_WINDOW_TOLERANCE);
        System.out.println(YELLOW + "  Reproduction rate : " + RESET
                + String.format("%.2f", 1.0 - this.crossover_rate - this.mutation_rate - this.hoist_rate));
        System.out.println(YELLOW + "  Seed              : " + RESET + this.seed);

        System.out.println(CYAN + "\n  -- Dataset -------------------------------" + RESET);
        if (dataset != null) {
            System.out.println(CYAN + "  File              : " + RESET + this.dataset.file);
            System.out.println(CYAN + "  Total rows        : " + RESET + this.dataset.getRecordCount());
            System.out.println(CYAN + "  Examples          : " + RESET + this.dataset.getExampleCount());
            System.out.println(CYAN + "  Train rows        : " + RESET + this.dataset.getTrainingInputs().length);
            System.out.println(CYAN + "  Validation rows   : " + RESET + this.dataset.getValidationInputs().length);
            System.out.println(CYAN + "  Test rows         : " + RESET + this.dataset.getTestingInputs().length);
        }
        System.out.println(CYAN + "  Features          : " + RESET + this.variableCount);

        System.out.println(GREEN + "\n  -- Results -------------------------------" + RESET);
        System.out.println(GREEN + "  Execution time    : " + RESET + (this.endTime - this.startTime) + " ms");
        System.out.println(GREEN + "  Best train RMSE   : " + RESET + String.format("%.6f", this.trainingRmse));
        System.out.println(GREEN + "  Best train fitness: " + RESET + String.format("%.6f", this.population[0].getFitness()));
        System.out.println(GREEN + "  Best valid RMSE   : " + RESET + String.format("%.6f", this.validationRmse));
        System.out.println(GREEN + "  Best program      : " + RESET + this.population[0]);

        System.out.println(BLUE + "\n  -- Top 5 Programs ------------------------" + RESET);
        int top = Math.min(5, this.population_size);
        for (int i = 0; i < top; i++) {
            Program program = this.population[i];
            String fitness = String.format("%.6f", program.getFitness());
            String color = i == 0 ? GREEN : (i < 3 ? CYAN : RESET);
            System.out.println(color + "  [" + (i + 1) + "] " + fitness + "  ->  " + program + RESET);
        }

        System.out.println(GREEN + "|================END OF RUN=================|" + RESET);
    }

    public Program getBestProgram() {
        ensureTrained();
        return population[0].deepCopy();
    }

    public double getBestTrainingMse() {
        ensureTrained();
        return trainingRmse * trainingRmse;
    }

    public double getBestTrainingRmse() {
        ensureTrained();
        return trainingRmse;
    }

    public double getBestValidationMse() {
        return validationRmse * validationRmse;
    }

    private void initialPopulation() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < this.population_size; i++) {
            Program candidate;
            do {
                int depth = this.min_depth + random.nextInt(this.max_depth - this.min_depth + 1);
                candidate = Program.growProgram(random, depth, variableCount);
            } while (seen.contains(candidate.toString()));

            seen.add(candidate.toString());
            this.population[i] = candidate;
        }
    }

    private void structureBasedSearch(double[][] inputs, double[] targets) {
        Program best = population[0].deepCopy();
        int generation = 0;

        while (generation < this.generations) {
            localSearchActive = false;
            int globalGenerations = Math.min(GLOBAL_SEARCH_GENERATIONS, this.generations - generation);
            for (int i = 0; i < globalGenerations; i++) {
                if (population[0].getFitness() < best.getFitness()) {
                    best = population[0].deepCopy();
                }

                replaceWorst(inputs, targets);
                sortPopulation();
                population[0] = best.deepCopy();
                sortPopulation();
                generation++;
            }

            Program globalArea = extractGlobalArea(population[0]);
            exploitedGlobalAreas[exploitedGlobalAreaCount] = globalArea;
            exploitedGlobalAreaCount++;
            fixPopulationToGlobalArea(globalArea, inputs, targets);

            localSearchActive = true;
            double bestBeforeLocal = population[0].getFitness();
            int staleGenerations = 0;

            while (generation < this.generations && staleGenerations < LOCAL_WINDOW_TOLERANCE) {
                replaceWorst(inputs, targets);
                sortPopulation();

                if (population[0].getFitness() < bestBeforeLocal) {
                    bestBeforeLocal = population[0].getFitness();
                    best = population[0].deepCopy();
                    staleGenerations = 0;
                } else {
                    staleGenerations++;
                }

                generation++;
            }
        }

        population[0] = best.deepCopy();
        sortPopulation();
    }

    private void evaluatePopulation(double[][] inputs, double[] targets) {
        for (Program program : this.population) {
            ensureValidProgram(program);
            program.setFitness(fitness(program, inputs, targets));
        }
    }

    private void replaceWorst() {
        replaceWorst(dataset.getTrainingInputs(), dataset.getTrainingTargets());
    }

    private void replaceWorst(double[][] inputs, double[] targets) {
        double choice = random.nextDouble();
        Program offspring;

        if (choice < this.crossover_rate) {
            offspring = crossover(selection(), selection());
        } else if (choice < this.crossover_rate + this.mutation_rate) {
            offspring = mutate(selection().deepCopy());
        } else if (choice < this.crossover_rate + this.mutation_rate + this.hoist_rate) {
            offspring = hoist(selection().deepCopy());
        } else {
            offspring = selection().deepCopy();
        }

        if (!offspring.isValid()) {
            offspring = Program.randomProgram(random, Math.max(2, this.min_depth), variableCount, false);
        }
        if (!localSearchActive && revisitsExploitedGlobalArea(offspring)) {
            return;
        }
        offspring.setFitness(fitness(offspring, inputs, targets));

        if (offspring.getFitness() < this.population[this.population_size - 1].getFitness()) {
            this.population[this.population_size - 1] = offspring;
        }
    }

    private Program selection() {
        int best = random.nextInt(this.population_size);

        for (int i = 1; i < tournamentSize; i++) {
            int candidate = random.nextInt(this.population_size);
            if (isBetter(this.population[candidate], this.population[best])) {
                best = candidate;
            }
        }

        return this.population[best];
    }

    private Program crossover(Program parentOne, Program parentTwo) {
        Node childRoot = parentOne.getRoot().copy();
        int replacementIndex = randomEditableIndex(parentOne);
        boolean needsFunction = replacementIndex == 0 || parentOne.isFunctionAt(replacementIndex);
        int remainingDepth = this.max_depth - parentOne.depthAt(replacementIndex) + 1;
        int donorIndex = randomCompatibleNodeIndex(parentTwo, needsFunction, remainingDepth);

        if (donorIndex == -1) {
            return parentOne.deepCopy();
        }

        Node donor = parentTwo.getRoot().getNode(donorIndex).copy();
        Node crossedRoot = childRoot.replaceNode(replacementIndex, donor);

        if (!isAcceptableRoot(crossedRoot)) {
            return parentOne.deepCopy();
        }

        return new Program(crossedRoot);
    }

    private Program mutate(Program program) {
        int targetIndex = randomEditableIndex(program);
        Node target = program.getRoot().getNode(targetIndex);
        Node replacement;

        if (target instanceof Function) {
            replacement = mutateFunctionNode((Function) target, program, targetIndex);
        } else {
            replacement = Program.randomTerminal(random, variableCount);
        }

        Node mutatedRoot = program.getRoot().replaceNode(targetIndex, replacement);

        if (!isAcceptableRoot(mutatedRoot)) {
            return program.deepCopy();
        }

        return new Program(mutatedRoot);
    }

    private Node mutateFunctionNode(Function target, Program program, int targetIndex) {
        int remainingDepth = this.max_depth - program.depthAt(targetIndex) + 1;

        if (remainingDepth < 2 || random.nextBoolean()) {
            return new Function(
                    Program.randomOperator(random),
                    target.getLeft().copy(),
                    target.getRight().copy());
        }

        int replacementDepth = Math.max(2, Math.min(this.mutationDepth, remainingDepth));
        return Program.randomProgram(random, replacementDepth, variableCount, false).getRoot();
    }

    private Program hoist(Program program) {
        if (program.size() <= 2) {
            return program.deepCopy();
        }

        int outerIndex = randomEditableIndex(program);
        Node outer = program.getRoot().getNode(outerIndex);

        if (outer.size() <= 1) {
            return program.deepCopy();
        }

        int innerIndex = 1 + random.nextInt(outer.size() - 1);
        Node inner = outer.getNode(innerIndex).copy();
        if (outerIndex == 0 && !(inner instanceof Function)) {
            return program.deepCopy();
        }
        Node hoistedRoot = program.getRoot().replaceNode(outerIndex, inner);
        if (!isAcceptableRoot(hoistedRoot)) {
            return program.deepCopy();
        }
        return new Program(hoistedRoot);
    }

    private double fitness(Program program, double[][] inputs, double[] targets) {
        return rmse(program, inputs, targets) + this.bloat_penalty * program.size();
    }

    private double rmse(Program program, double[][] inputs, double[] targets) {
        validateData(inputs, targets, inputs.length == 0 ? 0 : inputs[0].length);

        double total = 0.0;

        for (int i = 0; i < inputs.length; i++) {
            double prediction = program.evaluate(inputs[i]);
            if (!Double.isFinite(prediction)) {
                return Double.MAX_VALUE;
            }

            double error = prediction - targets[i];
            total += error * error;
        }

        return Math.sqrt(total / inputs.length);
    }

    private void sortPopulation() {
        Arrays.sort(this.population, this::comparePrograms);
    }

    private boolean isBetter(Program candidate, Program currentBest) {
        return comparePrograms(candidate, currentBest) < 0;
    }

    private int comparePrograms(Program left, Program right) {
        int fitnessComparison = Double.compare(left.getFitness(), right.getFitness());
        if (fitnessComparison != 0) {
            return fitnessComparison;
        }

        return Integer.compare(left.size(), right.size());
    }

    private int randomCompatibleNodeIndex(Program program, boolean needsFunction, int maxSubtreeDepth) {
        int selected = -1;
        int matches = 0;

        for (int i = 0; i < program.size(); i++) {
            Node candidate = program.getRoot().getNode(i);
            boolean roleMatches = needsFunction == (candidate instanceof Function);
            boolean depthFits = candidate.depth() <= maxSubtreeDepth;

            if (roleMatches && depthFits) {
                matches++;
                if (random.nextInt(matches) == 0) {
                    selected = i;
                }
            }
        }

        return selected;
    }

    private boolean isAcceptableRoot(Node root) {
        return root instanceof Function
                && root.size() >= Program.MIN_NODES
                && root.depth() <= this.max_depth;
    }

    private int randomEditableIndex(Program program) {
        if (!localSearchActive) {
            return random.nextInt(program.size());
        }

        int selected = -1;
        int matches = 0;

        for (int i = 0; i < program.size(); i++) {
            if (program.depthAt(i) > GLOBAL_AREA_DEPTH) {
                matches++;
                if (random.nextInt(matches) == 0) {
                    selected = i;
                }
            }
        }

        return selected == -1 ? program.size() - 1 : selected;
    }

    private Program extractGlobalArea(Program program) {
        return new Program(extractTopLevels(program.getRoot(), 1));
    }

    private void fixPopulationToGlobalArea(Program globalArea, double[][] inputs, double[] targets) {
        for (int i = 0; i < population.length; i++) {
            Node fixedRoot = graftGlobalArea(globalArea.getRoot(), population[i].getRoot(), 1);
            population[i] = new Program(fixedRoot);
            population[i].setFitness(fitness(population[i], inputs, targets));
        }
        sortPopulation();
    }

    private Node graftGlobalArea(Node globalNode, Node originalNode, int depth) {
        if (depth > GLOBAL_AREA_DEPTH) {
            return originalNode.copy();
        }

        if (globalNode instanceof Function && originalNode instanceof Function) {
            Function globalFunction = (Function) globalNode;
            Function originalFunction = (Function) originalNode;
            return new Function(
                    globalFunction.getOperator(),
                    graftGlobalArea(globalFunction.getLeft(), originalFunction.getLeft(), depth + 1),
                    graftGlobalArea(globalFunction.getRight(), originalFunction.getRight(), depth + 1));
        }

        return globalNode.copy();
    }

    private Node extractTopLevels(Node node, int depth) {
        if (!(node instanceof Function) || depth >= GLOBAL_AREA_DEPTH) {
            return node.copy();
        }

        Function function = (Function) node;
        return new Function(
                function.getOperator(),
                extractTopLevels(function.getLeft(), depth + 1),
                extractTopLevels(function.getRight(), depth + 1));
    }

    private boolean revisitsExploitedGlobalArea(Program candidate) {
        if (exploitedGlobalAreaCount == 0) {
            return false;
        }

        Program candidateArea = extractGlobalArea(candidate);
        for (int i = 0; i < exploitedGlobalAreaCount; i++) {
            if (similarity(candidateArea.getRoot(), exploitedGlobalAreas[i].getRoot(), 1) >= GLOBAL_SIMILARITY_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    private int similarity(Node left, Node right, int depth) {
        if (left == null || right == null || depth > GLOBAL_AREA_DEPTH) {
            return 0;
        }

        int score = sameNodeType(left, right) ? 1 : 0;
        if (left instanceof Function && right instanceof Function) {
            Function leftFunction = (Function) left;
            Function rightFunction = (Function) right;
            score += similarity(leftFunction.getLeft(), rightFunction.getLeft(), depth + 1);
            score += similarity(leftFunction.getRight(), rightFunction.getRight(), depth + 1);
        }

        return score;
    }

    private boolean sameNodeType(Node left, Node right) {
        if (left instanceof Function && right instanceof Function) {
            return ((Function) left).getOperator() == ((Function) right).getOperator();
        }
        return left.getClass().equals(right.getClass()) && left.toExpression().equals(right.toExpression());
    }

    private void validateData(double[][] inputs, double[] targets, int expectedVariables) {
        if (inputs == null || targets == null) {
            throw new IllegalArgumentException("Inputs and targets must not be null.");
        }
        if (inputs.length == 0 || inputs.length != targets.length) {
            throw new IllegalArgumentException("Inputs and targets must be non-empty and the same length.");
        }

        for (double[] input : inputs) {
            if (input == null || input.length != expectedVariables) {
                throw new IllegalArgumentException("Every input row must match the variable count.");
            }
        }
    }

    private void ensureDatasetAvailable() {
        if (dataset == null) {
            throw new IllegalStateException("This operation requires a Dataset.");
        }
    }

    private void ensureTrained() {
        if (population == null || population[0] == null || population[0].getFitness() == null) {
            throw new IllegalStateException("SBGP has not been trained yet.");
        }
    }

    private String[] defaultVariableNames(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "x" + i;
        }
        return names;
    }

    private void ensureValidProgram(Program program) {
        if (program == null || !program.isValid()) {
            throw new IllegalStateException("SBGP generated an invalid program. Programs must have a function root and at least "
                    + Program.MIN_NODES + " nodes.");
        }
    }
}
