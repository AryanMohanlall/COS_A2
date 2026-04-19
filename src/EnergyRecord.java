import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnergyRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDateTime timestamp;
    private final double load;
    private final double price;
    private final double solarGeneration;
    private final double windGeneration;
    private final double temperature;
    private final double humidity;

    public EnergyRecord(
            String timestamp,
            double load,
            double price,
            double solarGeneration,
            double windGeneration,
            double temperature,
            double humidity) {
        this.timestamp = LocalDateTime.parse(timestamp, FORMATTER);
        this.load = load;
        this.price = price;
        this.solarGeneration = solarGeneration;
        this.windGeneration = windGeneration;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public double getLoad() {
        return load;
    }

    public double getPrice() {
        return price;
    }

    public double getSolarGeneration() {
        return solarGeneration;
    }

    public double getWindGeneration() {
        return windGeneration;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getDay() {
        return timestamp.getDayOfMonth();
    }

    public double getMonth() {
        return timestamp.getMonthValue();
    }

    public double getYear() {
        return timestamp.getYear();
    }

    public double getHour() {
        return timestamp.getHour();
    }

    public double getMinute() {
        return timestamp.getMinute();
    }
}
