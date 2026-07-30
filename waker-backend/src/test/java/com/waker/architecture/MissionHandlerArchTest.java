package com.waker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture Decisions AD-2/AD-3: mission handlers are stateless strategy beans that must not
 * depend on commitment or user domain types.
 */
@AnalyzeClasses(packages = "com.waker", importOptions = ImportOption.DoNotIncludeTests.class)
class MissionHandlerArchTest {

  @ArchTest
  static final ArchRule missionHandlersDoNotDependOnCommitment =
      noClasses()
          .that()
          .resideInAPackage("com.waker.mission.internal..")
          .and()
          .haveSimpleNameEndingWith("MissionHandler")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.waker.commitment..")
          .because("AD-2: mission handlers must not depend on commitment types");

  @ArchTest
  static final ArchRule missionHandlersDoNotDependOnUser =
      noClasses()
          .that()
          .resideInAPackage("com.waker.mission.internal..")
          .and()
          .haveSimpleNameEndingWith("MissionHandler")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.waker.user..")
          .because("AD-3: mission handlers must not depend on user types");
}
