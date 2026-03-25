package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.core;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public final class OnnxSessionFactory {

    private static final OrtEnvironment ENV = OrtEnvironment.getEnvironment();

    private OnnxSessionFactory() {}

    public static OrtSession createSession(String modelPath) throws OrtException {
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.addCPU(true);
        return ENV.createSession(modelPath, options);
    }

    public static OrtEnvironment getEnvironment() {
        return ENV;
    }
}