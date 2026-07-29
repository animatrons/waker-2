package com.waker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture Decision AD-1: {@code com.waker.<module>.internal} is only reachable from the same
 * module root ({@code com.waker.<module>}). Cross-module callers must use package-root public
 * types.
 */
@AnalyzeClasses(packages = "com.waker", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchTest {

  @ArchTest
  static final ArchRule userInternalIsModulePrivate =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.waker.user..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.waker.user.internal..")
          .because("AD-1: user.internal is only accessible from com.waker.user");

  @ArchTest
  static final ArchRule commitmentInternalIsModulePrivate =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.waker.commitment..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.waker.commitment.internal..")
          .because("AD-1: commitment.internal is only accessible from com.waker.commitment");

  @ArchTest
  static final ArchRule missionInternalIsModulePrivate =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.waker.mission..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.waker.mission.internal..")
          .because("AD-1: mission.internal is only accessible from com.waker.mission");

  @ArchTest
  static final ArchRule penaltyInternalIsModulePrivate =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.waker.penalty..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.waker.penalty.internal..")
          .because("AD-1: penalty.internal is only accessible from com.waker.penalty");
}
