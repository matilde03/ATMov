package com.example.project_1v2;

public class ReadingItem {
    private final String timestamp;
    private final double temperature;
    private final double humidity;
    private final double luminosity;

    public ReadingItem(String timestamp, double temperature, double humidity, double luminosity) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.luminosity = luminosity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getLuminosity() {
        return luminosity;
    }
}
