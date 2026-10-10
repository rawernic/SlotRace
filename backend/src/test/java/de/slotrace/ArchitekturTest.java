package de.slotrace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Sichert die hexagonale Architektur ab: Abhängigkeiten zeigen immer nach innen. */
@AnalyzeClasses(packages = "de.slotrace", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitekturTest {

    @ArchTest
    static final ArchRule domaeneIstFrameworkfrei = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..",
                    "..application..", "..adapter..", "..config..");

    @ArchTest
    static final ArchRule anwendungKenntKeineAdapter = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule webAdapterNutztKeinePersistenz = noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..", "..application.port.out..");

    @ArchTest
    static final ArchRule persistenzAdapterNutztKeineEingangsports = noClasses().that()
            .resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.in..", "..application.port.in..",
                    "..application.service..");
}
