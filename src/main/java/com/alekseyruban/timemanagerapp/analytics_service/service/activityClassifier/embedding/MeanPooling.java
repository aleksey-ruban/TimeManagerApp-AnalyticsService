package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.embedding;

public final class MeanPooling {

    private MeanPooling() {}

    public static float[] apply(float[][] tokenEmbeddings, long[] attentionMask) {
        int tokens = tokenEmbeddings.length;
        int hiddenSize = tokenEmbeddings[0].length;

        float[] sum = new float[hiddenSize];
        int validTokens = 0;

        for (int i = 0; i < tokens; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < hiddenSize; j++) {
                    sum[j] += tokenEmbeddings[i][j];
                }
                validTokens++;
            }
        }

        for (int j = 0; j < hiddenSize; j++) {
            sum[j] /= validTokens;
        }

        return sum;
    }
}