package com.dani.bff.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.dani.bff");

    @Test
    void dtoClassesAreImmutableRecords() {
        ArchRule rule = classes()
                .that().resideInAPackage("..dto..")
                .should(beRecords());

        rule.check(classes);
    }

    @Test
    void apiLayerOnlyDependsOnAllowedLayers() {
        ArchRule rule = classes()
                .that().resideInAPackage("..api..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "com.dani.bff.api..",
                        "com.dani.bff.config..",
                        "com.dani.bff.dto..",
                        "com.dani.bff.error..",
                        "com.dani.bff.service..",
                        "io.swagger.v3.oas.annotations..",
                        "java..",
                        "org.springframework..",
                        "reactor..");

        rule.check(classes);
    }

    @Test
    void serviceLayerDoesNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..api..");

        rule.check(classes);
    }

    @Test
    void gatewayLayerDoesNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..gateway..")
                .should().dependOnClassesThat().resideInAPackage("..api..");

        rule.check(classes);
    }

    private static ArchCondition<JavaClass> beRecords() {
        return new ArchCondition<>("be Java records") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!item.reflect().isRecord()) {
                    events.add(SimpleConditionEvent.violated(item, item.getName() + " should be a Java record"));
                }
            }
        };
    }
}
