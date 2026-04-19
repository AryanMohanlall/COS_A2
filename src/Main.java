import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    record RunResult(double trainRMSE, double testRMSE, long executionMs, Program bestProgram) {}

    record ParamConfig(
        int populationSize,
        double crossoverRate,
        double mutationRate,
        double hoistRate,
        int minDepth,
        int maxDepth,
        int generations
    ) {
        @Override
        public String toString() {
            return String.format(
                    "pop=%d | cx=%.2f | mut=%.2f | hoist=%.2f | depth=%d-%d | gen=%d",
                    populationSize,
                    crossoverRate,
                    mutationRate,
                    hoistRate,
                    minDepth,
                    maxDepth,
                    generations);
        }
    }

    static final int RUNS = 15;

    public static void main(String[] args) throws Exception {
        Dataset ds = new Dataset("Residential_Energy_Dataset_UK- 2014-2020.csv");

        List<ParamConfig> configs = new ArrayList<>();

        // Baseline
        configs.add(new ParamConfig(100, 0.7, 0.15, 0.02, 2, 6, 100));

        // // Vary population size
        // configs.add(new ParamConfig(50, 0.7, 0.15, 0.05, 2, 6, 100));
        // configs.add(new ParamConfig(200, 0.7, 0.15, 0.05, 2, 6, 100));

        // // Vary max depth
        // configs.add(new ParamConfig(100, 0.7, 0.15, 0.05, 2, 4, 100));
        // configs.add(new ParamConfig(100, 0.7, 0.15, 0.05, 2, 8, 100));

        // // Vary operator balance
        // configs.add(new ParamConfig(100, 0.8, 0.10, 0.05, 2, 6, 100));
        // configs.add(new ParamConfig(100, 0.5, 0.30, 0.10, 2, 6, 100));

        System.out.println(configs.size() + " configs x " + RUNS + " runs each");

        for (int c = 0; c < configs.size(); c++) {
            ParamConfig config = configs.get(c);

            System.out.println("--------------------------------------------------------------------------------------------");
            System.out.println("Config " + (c + 1) + "/" + configs.size() + ": " + config);
            System.out.println("--------------------------------------------------------------------------------------------");

            List<RunResult> results = new ArrayList<>();

            for (int run = 0; run < RUNS; run++) {
                SBGP sbgp = new SBGP(
                        config.populationSize(),
                        config.crossoverRate(),
                        config.mutationRate(),
                        config.hoistRate(),
                        ds,
                        config.minDepth(),
                        config.maxDepth(),
                        config.generations());

                long start = System.currentTimeMillis();
                sbgp.train();
                long elapsed = System.currentTimeMillis() - start;

                double trainRMSE = sbgp.getBestTrainingRmse();
                double testRMSE = sbgp.test();

                results.add(new RunResult(trainRMSE, testRMSE, elapsed, sbgp.population[0]));

                System.out.println("  Run " + (run + 1) + "/" + RUNS
                        + " | Train: " + String.format("%.5f", trainRMSE)
                        + " | Test: " + String.format("%.5f", testRMSE)
                        + " | " + elapsed + "ms"
                        + " | Best: " + sbgp.population[0]);
            }

            double meanTrain = results.stream().mapToDouble(RunResult::trainRMSE).average().orElse(0);
            double meanTest = results.stream().mapToDouble(RunResult::testRMSE).average().orElse(0);
            double meanTime = results.stream().mapToDouble(RunResult::executionMs).average().orElse(0);
            double stdTrain = stdDev(results.stream().mapToDouble(RunResult::trainRMSE).toArray(), meanTrain);
            double stdTest = stdDev(results.stream().mapToDouble(RunResult::testRMSE).toArray(), meanTest);
            double bestTrain = results.stream().mapToDouble(RunResult::trainRMSE).min().orElse(0);
            double bestTest = results.stream().mapToDouble(RunResult::testRMSE).min().orElse(0);

            Program overallBest = results.stream()
                    .min((r1, r2) -> Double.compare(r1.trainRMSE, r2.trainRMSE))
                    .map(result -> result.bestProgram)
                    .orElse(null);

            System.out.println();
            System.out.println("  Mean Train RMSE        : " + String.format("%.5f", meanTrain));
            System.out.println("  Mean Test RMSE         : " + String.format("%.5f", meanTest));
            System.out.println("  StdDev Train / Test    : " + String.format("%.5f", stdTrain)
                    + " / " + String.format("%.5f", stdTest));
            System.out.println("  Best Train RMSE        : " + String.format("%.5f", bestTrain));
            System.out.println("  Best Test RMSE         : " + String.format("%.5f", bestTest));
            System.out.println("  Avg Time               : " + String.format("%.0f", meanTime) + "ms");
            System.out.println("  Best Program           : " + overallBest);
            System.out.println();
        }

        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("Experiment completed : " + ZonedDateTime.now());
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    private static double stdDev(double[] values, double mean) {
        double sum = 0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }
        return Math.sqrt(sum / values.length);
    }
}
