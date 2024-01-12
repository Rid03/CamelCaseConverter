package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConverterJava {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) {
        Properties prop = loadProperties();
        String directoryPath = prop.getProperty("json.folder.path");

        try {
            Path dir = Paths.get(directoryPath);
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(ConverterJava::processJsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() {
        Properties prop = new Properties();
        try (InputStream input = ConverterJava.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.out.println("Не удалось найти " + CONFIG_FILE);
                return null;
            }
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop;
    }

    private static void processJsonFile(Path path) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
            updateTagsToCamelCase(jsonMap);
            objectMapper.writeValue(path.toFile(), jsonMap);
            System.out.println("Файл успешно обработан: " + path);
        } catch (IOException e) {
            System.err.println("Ошибка при обработке файла: " + path);
            e.printStackTrace();
        }
    }

    private static void updateTagsToCamelCase(Map<String, Object> map) {
        map.forEach((key, value) -> {
            if ("tag".equals(key) && value instanceof Map) {
                updateSubMap((Map<String, String>) value);
            } else if (value instanceof Map) {
                updateTagsToCamelCase((Map<String, Object>) value);
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        updateTagsToCamelCase((Map<String, Object>) item);
                    }
                });
            }
        });
    }

    private static void updateSubMap(Map<String, String> subMap) {
        new HashMap<>(subMap).forEach((subKey, subValue) -> {
            if ("json".equals(subKey)) {
                subMap.put(subKey, toCamelCase(subValue));
            }
        });
    }

    private static String toCamelCase(String s) {
        StringBuilder builder = new StringBuilder();
        boolean nextUpper = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                builder.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
