package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.core.OnnxSessionFactory;

import java.nio.LongBuffer;
import java.util.Map;

public class TextEmbedder implements AutoCloseable {

    private final HuggingFaceTokenizer tokenizer;
    private final OrtSession session;
    private final OrtEnvironment env;

    public TextEmbedder(String tokenizerPath, String modelPath) throws Exception {
        this.tokenizer = HuggingFaceTokenizer.newInstance(
                java.nio.file.Paths.get(tokenizerPath)
        );
        this.env = OnnxSessionFactory.getEnvironment();
        this.session = OnnxSessionFactory.createSession(modelPath);
    }

    public float[] embed(String text) throws Exception {

        Encoding encoding = tokenizer.encode(text);

        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(inputIds),
                new long[]{1, inputIds.length});

             OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(
                     env,
                     LongBuffer.wrap(attentionMask),
                     new long[]{1, attentionMask.length});

             OrtSession.Result results = session.run(
                     Map.of(
                             "input_ids", inputIdsTensor,
                             "attention_mask", attentionMaskTensor
                     )
             )) {

            float[][][] output = (float[][][]) results.get(0).getValue();
            float[][] tokenEmbeddings = output[0];

            float[] pooled = MeanPooling.apply(tokenEmbeddings, attentionMask);
            return VectorUtils.l2Normalize(pooled);
        }
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}