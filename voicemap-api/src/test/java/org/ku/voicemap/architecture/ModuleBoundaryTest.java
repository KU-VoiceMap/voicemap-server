package org.ku.voicemap.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {

    private static JavaClasses apiClasses;

    @BeforeAll
    static void setUp() {
        apiClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.ku.voicemap");
    }

    @Test
    void apiModule_shouldNotDependOnGeminiImplementation() {
        noClasses()
            .that().resideInAPackage("org.ku.voicemap..")
            .and().resideOutsideOfPackage("org.ku.voicemap.ai.gemini..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.ku.voicemap.ai.gemini..")
            .as("API module should not depend on Gemini implementation details")
            .check(apiClasses);
    }

    @Test
    void aiCoreModule_shouldNotDependOnSpring() {
        JavaClasses coreClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.ku.voicemap.ai");

        noClasses()
            .that().resideInAPackage("org.ku.voicemap.ai.chat..")
            .or().resideInAPackage("org.ku.voicemap.ai.realtime..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .as("AI core interfaces should not depend on Spring framework")
            .check(coreClasses);
    }
}
