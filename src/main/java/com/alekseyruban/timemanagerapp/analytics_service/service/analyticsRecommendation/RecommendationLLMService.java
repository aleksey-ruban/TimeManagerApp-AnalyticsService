package com.alekseyruban.timemanagerapp.analytics_service.service.analyticsRecommendation;

import com.alekseyruban.timemanagerapp.analytics_service.service.analyticsRecommendation.model.PromptDefinition;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueParam;
import com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationLLMService {

    private final OllamaService ollamaService;

    public String generateSystemic(AnalyticsIssue issue) {
        String prompt = buildSystemicPrompt(issue);
        return ollamaService.ask(prompt);
    }

    public String generateNonSystemic(List<AnalyticsIssue> issues) {
        String prompt = buildNonSystemicPrompt(issues);
        return ollamaService.ask(prompt);
    }

    private String buildSystemicPrompt(AnalyticsIssue issue) {
        IssueCode code = issue.getCode();

        PromptDefinition definition = getDefinition(code);

        String dataBlock = buildSystemicDataBlock(issue, definition);

        return String.format("""
            Ты формируешь краткий аналитический текст для пользователя.

            Текст должен звучать естественно и обыденно, спокойно и персонально, обращаться к читателю можно только на «вы». 
            Первое предложение кратко описывает устойчивое состояние, второе — формулирует полезное действие, которое можно применить на практике.

            Формулировка должна быть личной, без упоминания автора рекомендации, без сложных канцеляризмов и без пересказа чисел из входных данных.

            Обнаружена проблема: %s (%s). 
            Проблема является системной, поскольку устойчиво проявлялась в течение последних 7 дней и формирует выраженный поведенческий паттерн.

            Нормальное значение: %s.

            Данные:
            %s

            Смысл рекомендации: %s

            Ответ — только 2 предложения на русском языке, простыми и живыми словами.
            """,
                definition.getProblemText(),
                code.name(),
                definition.getNormalValue(),
                dataBlock,
                definition.getRecommendationMeaning()
        );
    }

    private String buildNonSystemicPrompt(List<AnalyticsIssue> issues) {
        IssueCode code = issues.getFirst().getCode();

        PromptDefinition definition = getDefinition(code);

        String dataBlock = buildDataBlock(issues, definition);

        return String.format("""
            Ты формируешь краткий аналитический текст для пользователя.

            Текст должен звучать естественно и обыденно, спокойно и персонально, обращаться к читателю можно только на «вы». 
            Первое предложение кратко описывает наблюдаемое состояние, второе — формулирует полезное действие, которое можно применить на практике.

            Формулировка должна быть личной, без упоминания автора рекомендации, без сложных канцеляризмов и без пересказа чисел из входных данных.

            Обнаружена проблема: %s (%s). Проблема эпизодическая, не постоянная.

            Нормальное значение: %s.

            Данные:
            %s

            Смысл рекомендации: %s

            Ответ — только 2 предложения на русском языке, простыми и живыми словами.
            """,
                definition.getProblemText(),
                code.name(),
                definition.getNormalValue(),
                dataBlock,
                definition.getRecommendationMeaning()
        );
    }

    private PromptDefinition getDefinition(IssueCode code) {
        return switch (code) {
            case LOW_FOCUS -> new PromptDefinition(
                    "снижение концентрации внимания",
                    "focus score 0.55 и выше",
                    "при временном снижении концентрации полезно уменьшать отвлекающие факторы и делить работу на короткие последовательные участки",
                    List.of(IssueParam.FOCUS_SCORE)
            );

            case NO_BREAKS -> new PromptDefinition(
                    "недостаточное количество перерывов в течение рабочего дня",
                    "не менее 8 минут перерывов в час",
                    "регулярно делать короткие перерывы в течение рабочего времени, чтобы снизить накопление усталости и поддерживать устойчивую продуктивность",
                    List.of(IssueParam.BREAKS_MINUTES_PER_HOUR)
            );

            case TOO_MANY_TASK_SWITCHES -> new PromptDefinition(
                    "частое переключение между задачами",
                    "не более 6 переключений в час",
                    "сокращать количество одновременных переключений и завершать задачи более последовательно",
                    List.of(IssueParam.TASK_SWITCHES_PER_HOUR)
            );

            case EXCESSIVE_MULTITASKING -> new PromptDefinition(
                    "избыточная многозадачность",
                    "не более 10 минут пересечения задач в час",
                    "стараться выполнять задачи последовательно, чтобы снизить нагрузку на внимание",
                    List.of(IssueParam.MULTITASK_OVERLAP_MINUTES_PER_HOUR)
            );

            case FRAGMENTED_WORKDAY -> new PromptDefinition(
                    "фрагментированный рабочий день",
                    "не более 2 рабочих сессий",
                    "объединять рабочие задачи в более цельные блоки времени",
                    List.of(IssueParam.WORK_SESSION_COUNT)
            );

            case LONG_WORKDAY -> new PromptDefinition(
                    "увеличенная продолжительность рабочего дня",
                    "переработки до 20 минут",
                    "следить за своевременным завершением работы и оставлять время на восстановление",
                    List.of(IssueParam.OVERTIME_MINUTES)
            );

            case INSUFFICIENT_SLEEP -> new PromptDefinition(
                    "недостаточная продолжительность сна",
                    "не менее 420 минут сна",
                    "поддерживать стабильный и достаточный режим сна, чтобы сохранять энергию в течение дня",
                    List.of(IssueParam.SLEEP_DURATION)
            );

            case NO_DAYS_OFF -> new PromptDefinition(
                    "отсутствие регулярных выходных дней",
                    "не менее 2 выходных дней в неделю",
                    "регулярно оставлять дни без рабочей нагрузки, чтобы поддерживать восстановление и снижать накопление усталости",
                    List.of(IssueParam.TOTAL_DAYS_OFF_COUNT)
            );

            case IRREGULAR_SLEEP -> new PromptDefinition(
                    "нерегулярный режим сна",
                    "разница времени отхода ко сну не более 60 минут",
                    "стараться ложиться спать примерно в одно и то же время, чтобы режим сна оставался устойчивым",
                    List.of(IssueParam.SLEEP_START_STD_DEV)
            );
        };
    }

    private String buildSystemicDataBlock(AnalyticsIssue issue, PromptDefinition definition) {
        return issue.getParams().entrySet().stream()
                .filter(metric -> definition.getParameterNames().contains(metric.getKey()))
                .map(metric -> metric.getKey() + ": " + metric.getValue())
                .collect(Collectors.joining(", "));
    }

    private String buildDataBlock(List<AnalyticsIssue> issues, PromptDefinition definition) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < issues.size(); i++) {
            AnalyticsIssue issue = issues.get(i);

            sb.append("День ").append(i + 1).append(": ");

            String values = issue.getParams().entrySet().stream()
                    .filter(metric -> definition.getParameterNames().contains(metric.getKey()))
                    .map(metric -> metric.getKey() + ": " + metric.getValue())
                    .collect(Collectors.joining(", "));

            sb.append(values).append("\n");
        }

        return sb.toString();
    }
}