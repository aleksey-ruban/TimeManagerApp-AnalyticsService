package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.embedding;

public final class VectorUtils {

    private VectorUtils() {}

    public static float[] l2Normalize(float[] vector) {
        double sumSquares = 0.0;

        for (float v : vector) {
            sumSquares += v * v;
        }

        double norm = Math.sqrt(sumSquares);

        if (norm == 0.0) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }

        return vector;
    }
}