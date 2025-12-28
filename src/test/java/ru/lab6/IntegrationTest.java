package ru.lab6;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.lab6.model.AnalysisResult;
import ru.lab6.parser.sequential.SequentialParser;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String TEST_FILE = "data/test_10k.json";
    private static AnalysisResult expected;

    @BeforeAll
    static void setUp() throws IOException {
        // Генерируем тестовый файл один раз (если его нет)
        File file = new File(TEST_FILE);
        if (!file.exists()) {
            new File("data").mkdirs();
            ru.lab6.generator.DataGenerator.generateJson(10_000, TEST_FILE);
        }

        // Последовательный парсер — эталон
        expected = new SequentialParser().parse(file);
    }

    @Test
    void sequentialParser_shouldMatchItself() throws IOException {
        AnalysisResult actual = new SequentialParser().parse(new File(TEST_FILE));
        assertResultsEqual(expected, actual);
    }

    // TODO: Раскомментируйте, как только появятся другие парсеры:
    /*
    @Test
    void forkJoinParser_shouldMatchSequential() throws Exception {
        AnalysisResult actual = new ru.lab6.parser.forkjoin.ForkJoinParser().parse(new File(TEST_FILE));
        assertResultsEqual(expected, actual);
    }

    @Test
    void completableFutureParser_shouldMatchSequential() throws Exception {
        AnalysisResult actual = new ru.lab6.parser.async.CompletableFutureParser().parse(new File(TEST_FILE));
        assertResultsEqual(expected, actual);
    }

    @Test
    void producerConsumerParser_shouldMatchSequential() throws Exception {
        AnalysisResult actual = new ru.lab6.parser.producer.ProducerConsumerParser().parse(new File(TEST_FILE));
        assertResultsEqual(expected, actual);
    }
    */

    private static void assertResultsEqual(AnalysisResult expected, AnalysisResult actual) {
        assertEquals(expected.cityDistribution, actual.cityDistribution, "City distribution mismatch");
        assertEquals(expected.avgLikesPerUser, actual.avgLikesPerUser, 0.001, "Avg likes mismatch");
        assertEquals(expected.top10Tags.size(), actual.top10Tags.size(), "Top tags count mismatch");
        for (int i = 0; i < expected.top10Tags.size(); i++) {
            var e = expected.top10Tags.get(i);
            var a = actual.top10Tags.get(i);
            assertEquals(e.tag, a.tag, "Tag name mismatch at #" + i);
            assertEquals(e.count, a.count, "Tag count mismatch at #" + i);
        }
    }
}