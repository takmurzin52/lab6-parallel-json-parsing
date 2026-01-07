package ru.lab6.parser.sequential;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.lab6.model.AnalysisResult;
import ru.lab6.model.Post;
import ru.lab6.model.User;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SequentialParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisResult parse(File jsonFile) throws IOException {
        // 1. Читаем ВЕСЬ файл в память (последовательно, как в ТЗ)
        List<User> users = mapper.readValue(jsonFile, mapper.getTypeFactory()
                .constructCollectionType(List.class, User.class));

        // 2. Агрегация
        Map<String, Long> cityDistribution = new HashMap<>();
        long totalLikes = 0;
        Map<String, Long> tagCounts = new HashMap<>();

        for (User user : users) {
            // Города
            cityDistribution.merge(user.city, 1L, Long::sum);

            // Лайки
            for (Post post : user.posts) {
                totalLikes += post.likes;

                // Теги
                for (String tag : post.tags) {
                    tagCounts.merge(tag, 1L, Long::sum);
                }
            }
        }

        // 3. Формируем результат
        AnalysisResult result = new AnalysisResult();
        result.cityDistribution = cityDistribution;
        result.avgLikesPerUser = users.isEmpty() ? 0.0 : (double) totalLikes / users.size();

        // Топ-10 тегов
        result.top10Tags = tagCounts.entrySet().stream()
                .map(e -> new AnalysisResult.TagCount(e.getKey(), e.getValue()))
                .sorted()
                .limit(10)
                .collect(Collectors.toList());

        return result;
    }
}