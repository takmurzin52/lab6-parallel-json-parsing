package ru.lab6.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalysisResult {

    public Map<String, Long> cityDistribution = new HashMap<>();
    public Map<String, Long> tagCounts = new HashMap<>();


    public long totalLikes = 0;
    public int userCount = 0;


    public double avgLikesPerUser;
    public List<TagCount> top10Tags;


    public static class TagCount implements Comparable<TagCount> {
        public String tag;
        public long count;

        public TagCount(String tag, long count) {
            this.tag = tag;
            this.count = count;
        }

        @Override
        public int compareTo(TagCount o) {
            return Long.compare(o.count, this.count);
        }
    }


    public void merge(AnalysisResult other) {
        if (other == null) return;

        // 1. Слияние городов
        other.cityDistribution.forEach((city, count) ->
                this.cityDistribution.merge(city, count, Long::sum));

        // 2. Слияние тегов
        other.tagCounts.forEach((tag, count) ->
                this.tagCounts.merge(tag, count, Long::sum));

        // 3. Слияние лайков и пользователей
        this.totalLikes += other.totalLikes;
        this.userCount += other.userCount;
    }

    public void finalizeStats() {
        this.avgLikesPerUser = userCount == 0 ? 0 : (double) totalLikes / userCount;

        this.top10Tags = tagCounts.entrySet().stream()
                .map(e -> new TagCount(e.getKey(), e.getValue()))
                .sorted()
                .limit(10)
                .collect(Collectors.toList());
    }
}