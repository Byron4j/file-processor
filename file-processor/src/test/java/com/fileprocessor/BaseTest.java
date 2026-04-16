package com.fileprocessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    protected static final String TEST_DIR = "./test-files/";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test directory
        new File(TEST_DIR).mkdirs();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test files
        Path testPath = Path.of(TEST_DIR);
        if (Files.exists(testPath)) {
            try (Stream<Path> walk = Files.walk(testPath)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    protected String getTestFilePath(String filename) {
        return TEST_DIR + filename;
    }
}
