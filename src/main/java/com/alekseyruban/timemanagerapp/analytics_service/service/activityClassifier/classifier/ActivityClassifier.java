package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.classifier;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.core.OnnxSessionFactory;

import java.util.Collections;

public class ActivityClassifier implements AutoCloseable {

    private final OrtSession session;
    private final OrtEnvironment env;
    private final String inputName;

    public ActivityClassifier(String modelPath) throws Exception {
        this.env = OnnxSessionFactory.getEnvironment();
        this.session = OnnxSessionFactory.createSession(modelPath);
        this.inputName = session.getInputNames().iterator().next();
    }

    public int predict(float[] embedding) throws Exception {

        float[][] input = new float[1][embedding.length];
        input[0] = embedding;

        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
             OrtSession.Result results =
                     session.run(Collections.singletonMap(inputName, tensor))) {

            long[] predictedLabel = (long[]) results.get(0).getValue();
            return (int) predictedLabel[0];
        }
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}