package ru.lab6.model;

import java.util.List;
import java.util.Map;

public class AnalysisResult {
    public Map<String, Long> cityDistribution;   // город -> количество пользователей
    public double avgLikesPerUser;                // среднее количество лайков на пользователя
    public List<TagCount> top10Tags;              // топ-10 тегов

    // Вложенный класс для хранения пар "тег - количество"
    public static class TagCount implements Comparable<TagCount> {
        public String tag;
        public long count;

        public TagCount(String tag, long count) {
            this.tag = tag;
            this.count = count;
        }

        @Override
        public int compareTo(TagCount o) {
            return Long.compare(o.count, this.count); // по убыванию
        }
    }
}