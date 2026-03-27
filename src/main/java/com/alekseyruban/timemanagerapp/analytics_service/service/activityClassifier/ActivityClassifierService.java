package com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.ActivitySnapshotDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategorySnapshotDto;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.classifier.ActivityClassifier;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.embedding.TextEmbedder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityClassifierService {

    private final TextEmbedder embedder;
    private final ActivityClassifier classifier;

    public ActivityClassifierService() {
        try {
            String tokenizerPath = copyResourceToTemp("aiModels/multilingualE5base/tokenizer.json");
            String modelPath = copyResourceToTemp("aiModels/multilingualE5base/model_O4.onnx");
            String classifierPath = copyResourceToTemp("aiModels/activityClassifier/activity_classifier.onnx");

            embedder = new TextEmbedder(tokenizerPath, modelPath);
            classifier = new ActivityClassifier(classifierPath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String copyResourceToTemp(String resourcePath) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        String suffix = resourcePath.contains(".") ? resourcePath.substring(resourcePath.lastIndexOf('.')) : null;
        Path tempFile = Files.createTempFile("temp-", suffix);
        tempFile.toFile().deleteOnExit(); // удалим при выходе

        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toAbsolutePath().toString();
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
