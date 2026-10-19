package com.example.monthlysales.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * domain 層が枠組みに依存していないことを確かめる。
 *
 * <p>Spring・MyBatis・Spring Batch の型を domain が取り込むと、業務の決まりが
 * 起動方式に縛られてしまう。これを取り違えやすいので、作りの決まりとして機械的に確かめる。
 */
class DomainLayerDependencyTest {

    private static final Path DOMAIN_ROOT =
            Path.of("src/main/java/com/example/monthlysales/domain");

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "import org.springframework.",
            "import org.apache.ibatis.",
            "import org.mybatis.");

    @Test
    void domainはSpringもMyBatisもSpringBatchも取り込まない() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(DOMAIN_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.strip();
                    if (FORBIDDEN_PREFIXES.stream().anyMatch(trimmed::startsWith)) {
                        violations.add(file + " -> " + trimmed);
                    }
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void 確かめる対象のdomainのソースが実際にある() throws IOException {
        try (Stream<Path> files = Files.walk(DOMAIN_ROOT)) {
            assertThat(files.filter(p -> p.toString().endsWith(".java")).count())
                    .isGreaterThan(10);
        }
    }
}
