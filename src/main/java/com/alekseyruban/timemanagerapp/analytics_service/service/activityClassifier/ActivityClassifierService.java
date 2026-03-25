package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.ActivitySnapshotDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategorySnapshotDto;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.classifier.ActivityClassifier;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.embedding.TextEmbedder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityClassifierService {

    private final String tokenizerPath = "src/main/resources/aiModels/multilingualE5base/tokenizer.json";
    private final String modelPath = "src/main/resources/aiModels/multilingualE5base/model_O4.onnx";
    private final String classifierPath = "src/main/resources/aiModels/activityClassifier/activity_classifier.onnx";

    private final TextEmbedder embedder;
    private final ActivityClassifier classifier;

    public ActivityClassifierService() {
        try {
            embedder = new TextEmbedder(tokenizerPath, modelPath);
            classifier = new ActivityClassifier(classifierPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ActivityClass classifyActivity(ActivitySnapshotDto activity, CategorySnapshotDto category) {

        boolean categoryExists = category != null;
        if (categoryExists) {
            Optional<ActivityClass> forcedActivityClass = resolveForcedActivityClass(category.getCode());

            if (forcedActivityClass.isPresent()) {
                return forcedActivityClass.get();
            }
        }

        String formatted = "query: " + activity.getName() +
                (categoryExists ? ", " + category.getBaseName() : "");

        try {
            float[] embedding = embedder.embed(formatted);
            int labelId = classifier.predict(embedding);
            return ActivityClass.fromId(labelId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ActivityClass> resolveForcedActivityClass(CategoryCode code) {
        if (code == null) {
            return Optional.empty();
        }

        return switch (code) {
            case WORK -> Optional.of(ActivityClass.WORK);
            case WORKOUT, LEISURE, ENTERTAINMENT, SOCIAL_NETWORK -> Optional.of(ActivityClass.LEISURE);
            case SLEEP -> Optional.of(ActivityClass.REST);
        };
    }
}
