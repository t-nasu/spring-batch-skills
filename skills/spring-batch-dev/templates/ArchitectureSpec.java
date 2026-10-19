package __ROOT_PACKAGE__;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * アーキテクチャの決まりを検証する。アプリのルートパッケージ（@SpringBootApplication と同じパッケージ）に置く。
 */
@AnalyzeClasses(packagesOf = ArchitectureSpec.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureSpec {

    private static final String ROOT = ArchitectureSpec.class.getPackageName();
    private static final String BATCH = ROOT + ".batch..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";

    /** 依存の向きは batch → application → domain ← infrastructure のみ。 */
    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("batch").definedBy(BATCH)
            .layer("application").definedBy(APPLICATION)
            .layer("domain").definedBy(DOMAIN)
            .layer("infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("batch").mayNotBeAccessedByAnyLayer()
            .whereLayer("application").mayOnlyBeAccessedByLayers("batch")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("batch", "application", "infrastructure")
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer();

    /** domain は Spring・MyBatis・Spring Batch に依存しない。 */
    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.apache.ibatis..", "org.mybatis..");

    /** application 層のクラス名に Service を使わない（UseCase とする）。 */
    @ArchTest
    static final ArchRule noServiceInApplication = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().haveSimpleNameEndingWith("Service");

    /**
     * domain に public な setter を置かない。
     * Lombok の @Data / @Setter はクラスファイルに残らないため、生成される setter で検出する。
     */
    @ArchTest
    static final ArchRule noPublicSettersInDomain = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
            .and().haveNameMatching("set[A-Z].*")
            .should().notBePublic()
            .allowEmptyShould(true);

    private static final Pattern LOMBOK_DATA_OR_SETTER =
            Pattern.compile("@(lombok\\.)?(Data|Setter)\\b|import\\s+lombok\\.(Data|Setter)\\s*;");

    /** domain のソースで Lombok の @Data / @Setter を使わない。 */
    @ArchTest
    static void noLombokDataOrSetterInDomain(JavaClasses classes) throws IOException {
        List<String> violations = new ArrayList<>();
        for (JavaClass javaClass : classes.that(DescribedPredicate.describe(
                "in domain", c -> c.getPackageName().startsWith(ROOT + ".domain")))) {
            if (javaClass.getEnclosingClass().isPresent()) {
                continue;
            }
            Path source = Path.of("src/main/java", javaClass.getName().replace('.', '/') + ".java");
            if (Files.exists(source) && LOMBOK_DATA_OR_SETTER.matcher(Files.readString(source)).find()) {
                violations.add(source.toString());
            }
        }
        if (!violations.isEmpty()) {
            throw new AssertionError("domain で Lombok の @Data / @Setter を使っている: " + violations);
        }
    }
}
