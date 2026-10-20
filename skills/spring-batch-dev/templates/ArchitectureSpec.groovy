package __ROOT_PACKAGE__

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import static com.tngtech.archunit.library.Architectures.layeredArchitecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Shared
import spock.lang.Specification

/**
 * アーキテクチャの決まりを検証する。アプリのルートパッケージ（@SpringBootApplication と同じパッケージ）に置く。
 */
class ArchitectureSpec extends Specification {

    static final String ROOT = ArchitectureSpec.packageName
    static final String BATCH = "${ROOT}.batch.."
    static final String APPLICATION = "${ROOT}.application.."
    static final String DOMAIN = "${ROOT}.domain.."
    static final String INFRASTRUCTURE = "${ROOT}.infrastructure.."

    @Shared
    JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT)

    def "依存の向きは batch → application → domain ← infrastructure のみ"() {
        given:
        def rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("batch").definedBy(BATCH)
                .layer("application").definedBy(APPLICATION)
                .layer("domain").definedBy(DOMAIN)
                .layer("infrastructure").definedBy(INFRASTRUCTURE)
                .whereLayer("batch").mayNotBeAccessedByAnyLayer()
                .whereLayer("application").mayOnlyBeAccessedByLayers("batch")
                .whereLayer("domain").mayOnlyBeAccessedByLayers("batch", "application", "infrastructure")
                .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()

        when:
        rule.check(classes)

        then:
        noExceptionThrown()
    }

    def "domain は Spring・MyBatis・Spring Batch に依存しない"() {
        given:
        def rule = noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "org.apache.ibatis..", "org.mybatis..")

        when:
        rule.check(classes)

        then:
        noExceptionThrown()
    }

    def "application 層のクラス名に Service を使わない（UseCase とする）"() {
        given:
        def rule = noClasses()
                .that().resideInAPackage(APPLICATION)
                .should().haveSimpleNameEndingWith("Service")

        when:
        rule.check(classes)

        then:
        noExceptionThrown()
    }

    // Lombok の @Data / @Setter はクラスファイルに残らないため、生成される setter で検出する
    def "domain に public な setter を置かない"() {
        given:
        def rule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
                .and().haveNameMatching("set[A-Z].*")
                .should().notBePublic()
                .allowEmptyShould(true)

        when:
        rule.check(classes)

        then:
        noExceptionThrown()
    }

    def "domain のソースで Lombok の @Data / @Setter を使わない"() {
        given:
        def pattern = ~/@(lombok\.)?(Data|Setter)\b|import\s+lombok\.(Data|Setter)\s*;/

        when:
        def violations = classes
                .findAll { it.packageName.startsWith("${ROOT}.domain") && !it.enclosingClass.present }
                .collect { Path.of("src/main/java", it.name.replace('.', '/') + ".java") }
                .findAll { Files.exists(it) && Files.readString(it) =~ pattern }

        then:
        violations.empty
    }
}
