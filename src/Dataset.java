import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Dataset {
    private static final int VALUES_PER_DAY = 96;
    private static final int PREVIOUS_DAYS = 7;
    private static final double TRAINING_RATIO = 0.80;
    private static final String[] FEATURE_NAMES = {
            "price_t",
            "solar_t",
            "wind_t",
            "temp_t",
            "humidity_t",
            "day",
            "month",
            "year",
            "hour",
            "minute",
            "load_d1",
            "price_d1",
            "solar_d1",
            "wind_d1",
            "temp_d1",
            "humidity_d1",
            "load_d2",
            "price_d2",
            "solar_d2",
            "wind_d2",
            "temp_d2",
            "humidity_d2",
            "load_d3",
            "price_d3",
            "solar_d3",
            "wind_d3",
            "temp_d3",
            "humidity_d3",
            "load_d4",
            "price_d4",
            "solar_d4",
            "wind_d4",
            "temp_d4",
            "humidity_d4",
            "load_d5",
            "price_d5",
            "solar_d5",
            "wind_d5",
            "temp_d5",
            "humidity_d5",
            "load_d6",
            "price_d6",
            "solar_d6",
            "wind_d6",
            "temp_d6",
            "humidity_d6",
            "load_d7",
            "price_d7",
            "solar_d7",
            "wind_d7",
            "temp_d7",
            "humidity_d7"
    };

    public final String file;

    private final double[][] trainingInputs;
    private final double[] trainingTargets;
    private final double[][] validationInputs;
    private final double[] validationTargets;
    private final double[][] testingInputs;
    private final double[] testingTargets;
    private final int recordCount;
    private final int exampleCount;
    private final int variableCount;

    public Dataset(String file) throws IOException {
        this(Path.of(file));
    }

    public Dataset(Path path) throws IOException {
        this(loadRecords(path), path.toString());
    }

    private Dataset(List<EnergyRecord> records, String file) {
        this(buildInputs(records), buildTargets(records), records.size(), file);
    }

    private Dataset(double[][] inputs, double[] targets, int recordCount, String file) {
        if (inputs.length == 0) {
            throw new IllegalArgumentException("Dataset does not contain enough records to create examples.");
        }

        this.file = file;
        int trainSize = (int) (recordCount * TRAINING_RATIO) - firstUsableRecordIndex();
        if (trainSize <= 0 || trainSize >= inputs.length) {
            trainSize = (int) (inputs.length * TRAINING_RATIO);
        }
        int testSize = inputs.length - trainSize;

        this.trainingInputs = copyInputs(inputs, 0, trainSize);
        this.trainingTargets = copyTargets(targets, 0, trainSize);
        this.validationInputs = copyInputs(inputs, trainSize, testSize);
        this.validationTargets = copyTargets(targets, trainSize, testSize);
        this.testingInputs = copyInputs(inputs, trainSize, testSize);
        this.testingTargets = copyTargets(targets, trainSize, testSize);
        this.recordCount = recordCount;
        this.exampleCount = inputs.length;
        this.variableCount = inputs[0].length;
        Variable.setNames(getFeatureNames());
    }

    public static Dataset fromCsv(Path path) throws IOException {
        return new Dataset(path);
    }

    public double[][] getTrainingInputs() {
        return trainingInputs;
    }

    public double[] getTrainingTargets() {
        return trainingTargets;
    }

    public double[][] getValidationInputs() {
        return validationInputs;
    }

    public double[] getValidationTargets() {
        return validationTargets;
    }

    public double[][] getTestingInputs() {
        return testingInputs;
    }

    public double[] getTestingTargets() {
        return testingTargets;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getExampleCount() {
        return exampleCount;
    }

    public int getVariableCount() {
        return variableCount;
    }

    public String[] getFeatureNames() {
        return FEATURE_NAMES.clone();
    }

    private static List<EnergyRecord> loadRecords(Path path) throws IOException {
        List<EnergyRecord> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",");
                records.add(new EnergyRecord(
                        parts[0],
                        parseDouble(parts[1]),
                        parseDouble(parts[2]),
                        parseDouble(parts[3]),
                        parseDouble(parts[4]),
                        parseDouble(parts[5]),
                        parseDouble(parts[6])));
            }
        }

        return records;
    }

    private static double[][] buildInputs(List<EnergyRecord> records) {
        List<double[]> inputs = new ArrayList<>();
        int start = firstUsableRecordIndex();

        for (int i = start; i < records.size(); i++) {
            double[] row = new double[getVariableCountFromConfiguration()];
            int column = 0;
            EnergyRecord current = records.get(i);

            row[column] = current.getPrice();
            column++;
            row[column] = current.getSolarGeneration();
            column++;
            row[column] = current.getWindGeneration();
            column++;
            row[column] = current.getTemperature();
            column++;
            row[column] = current.getHumidity();
            column++;
            row[column] = current.getDay();
            column++;
            row[column] = current.getMonth();
            column++;
            row[column] = current.getYear();
            column++;
            row[column] = current.getHour();
            column++;
            row[column] = current.getMinute();
            column++;

            for (int day = 1; day <= PREVIOUS_DAYS; day++) {
                EnergyRecord previousDay = records.get(i - day * VALUES_PER_DAY);

                row[column] = previousDay.getLoad();
                column++;
                row[column] = previousDay.getPrice();
                column++;
                row[column] = previousDay.getSolarGeneration();
                column++;
                row[column] = previousDay.getWindGeneration();
                column++;
                row[column] = previousDay.getTemperature();
                column++;
                row[column] = previousDay.getHumidity();
                column++;
            }

            inputs.add(row);
        }

        return inputs.toArray(new double[0][]);
    }

    private static double[] buildTargets(List<EnergyRecord> records) {
        int start = firstUsableRecordIndex();
        double[] targets = new double[records.size() - start];

        for (int i = start; i < records.size(); i++) {
            targets[i - start] = records.get(i).getLoad();
        }

        return targets;
    }

    private static int firstUsableRecordIndex() {
        return PREVIOUS_DAYS * VALUES_PER_DAY;
    }

    private static int getVariableCountFromConfiguration() {
        return FEATURE_NAMES.length;
    }

    private static double[][] copyInputs(double[][] inputs, int start, int length) {
        double[][] result = new double[length][];

        for (int i = 0; i < length; i++) {
            result[i] = inputs[start + i];
        }

        return result;
    }

    private static double[] copyTargets(double[] targets, int start, int length) {
        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            result[i] = targets[start + i];
        }

        return result;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }
}
