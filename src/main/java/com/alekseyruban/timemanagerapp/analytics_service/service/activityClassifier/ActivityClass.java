package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier;

public enum ActivityClass {

    WORK(0, "WORK"),
    LEISURE(1, "LEISURE"),
    REST(2, "REST");

    private final int id;
    private final String label;

    ActivityClass(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static ActivityClass fromId(int id) {
        for (ActivityClass c : values()) {
            if (c.id == id) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown class id: " + id);
    }

    public String getLabel() {
        return label;
    }
}