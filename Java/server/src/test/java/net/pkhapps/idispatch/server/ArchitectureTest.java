package net.pkhapps.idispatch.server;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Keeps the dependencies between the feature packages pointing one way, so that each feature can be changed without
 * touching the features it depends on.
 * <p>
 * The jOOQ metadata classes in {@code jooq} are generated from the whole schema and may be used by every feature; which
 * tables a feature queries is a matter of review, not of these rules.
 */
@AnalyzeClasses(packages = "net.pkhapps.idispatch.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "net.pkhapps.idispatch.server";
    private static final String[] FEATURES = {
            BASE + ".municipality..",
            BASE + ".assignmenttype..",
            BASE + ".assignment..",
            BASE + ".resource..",
            BASE + ".dispatch..",
            BASE + ".report..",
            BASE + ".workstation..",
    };

    /**
     * {@code municipality} and {@code assignmenttype} ← {@code assignment} ← {@code resource} ← {@code dispatch} ←
     * {@code workstation}, with {@code report} on {@code assignment} and {@code resource}. Only dependencies between the
     * features count here; the rules below cover the packages around them.
     */
    @ArchTest
    static final ArchRule featuresDependOnEachOtherOneWay = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Municipality").definedBy(BASE + ".municipality..")
            .layer("AssignmentType").definedBy(BASE + ".assignmenttype..")
            .layer("Assignment").definedBy(BASE + ".assignment..")
            .layer("Resource").definedBy(BASE + ".resource..")
            .layer("Dispatch").definedBy(BASE + ".dispatch..")
            .layer("Report").definedBy(BASE + ".report..")
            .layer("Workstation").definedBy(BASE + ".workstation..")
            .whereLayer("Municipality").mayOnlyBeAccessedByLayers("Assignment", "Resource", "Dispatch", "Report", "Workstation")
            .whereLayer("AssignmentType").mayOnlyBeAccessedByLayers("Assignment", "Resource", "Dispatch", "Report", "Workstation")
            .whereLayer("Assignment").mayOnlyBeAccessedByLayers("Resource", "Dispatch", "Report", "Workstation")
            .whereLayer("Resource").mayOnlyBeAccessedByLayers("Dispatch", "Report", "Workstation")
            .whereLayer("Dispatch").mayOnlyBeAccessedByLayers("Workstation")
            .whereLayer("Report").mayNotBeAccessedByAnyLayer()
            .whereLayer("Workstation").mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule sharedKnowsNoFeature = noClasses()
            .that().resideInAPackage(BASE + ".shared..")
            .should().dependOnClassesThat().resideInAnyPackage(FEATURES)
            .orShould().dependOnClassesThat().resideInAPackage(BASE + ".ui..");

    @ArchTest
    static final ArchRule securityKnowsNoFeature = noClasses()
            .that().resideInAPackage(BASE + ".security..")
            .should().dependOnClassesThat().resideInAnyPackage(FEATURES);

    /**
     * {@code RootLayout} builds the drawer from the views of every feature; everything else in {@code ui} is shared by
     * the features and must not know them.
     */
    @ArchTest
    static final ArchRule onlyTheRootLayoutKnowsTheFeatures = noClasses()
            .that().resideInAPackage(BASE + ".ui..")
            .and().doNotHaveFullyQualifiedName(BASE + ".ui.RootLayout")
            .should().dependOnClassesThat().resideInAnyPackage(FEATURES);
}
