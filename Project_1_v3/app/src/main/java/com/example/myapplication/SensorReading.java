package com.example.myapplication;

public class SensorReading {

    private final float value;
    private final long timestamp;

    public SensorReading(float value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }
    public float getValue() {
        return value;
    }
    public long getTimestamp() {
        return timestamp;
    }
}
