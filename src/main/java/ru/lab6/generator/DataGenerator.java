package ru.lab6.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.lab6.model.Post;
import ru.lab6.model.User;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DataGenerator {

    private static final String[] CITIES = {"Moscow", "SPb", "Novosibirsk", "Yekaterinburg", "Kazan"};
    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace"};
    private static final String[] TAGS = {"tech", "science", "music", "art", "design", "sport", "travel", "food"};

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: DataGenerator <count> <output.json>");
            System.exit(1);
        }
        int count = Integer.parseInt(args[0]);
        String outputPath = args[1];

        generateJson(count, outputPath);
        System.out.println("✅ Generated " + count + " users → " + outputPath);
    }

    public static void generateJson(int count, String outputPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // для читаемости (можно убрать для скорости)

        List<User> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            users.add(generateUser(i));
        }

        // Записываем в файл
        mapper.writeValue(new File(outputPath), users);
    }

    private static User generateUser(int id) {
        User u = new User();
        u.id = id;
        u.name = randomElement(NAMES);
        u.age = 18 + ThreadLocalRandom.current().nextInt(48); // 18–65
        u.city = randomElement(CITIES);
        u.posts = new ArrayList<>();

        int postCount = ThreadLocalRandom.current().nextInt(21); // 0–20
        for (int j = 0; j < postCount; j++) {
            Post p = new Post();
            p.likes = ThreadLocalRandom.current().nextInt(51); // 0–50
            int tagCount = 1 + ThreadLocalRandom.current().nextInt(4); // 1–4
            p.tags = new ArrayList<>();
            for (int k = 0; k < tagCount; k++) {
                p.tags.add(randomElement(TAGS));
            }
            u.posts.add(p);
        }
        return u;
    }

    private static <T> T randomElement(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
}