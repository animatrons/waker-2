package com.waker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture Decisions AD-2/AD-3: penalty handlers are stateless strategy beans that must not
 * depend on commitment or user domain types.
 */
@AnalyzeClasses(packages = "com.waker", importOptions = ImportOption.DoNotIncludeTests.class)
class PenaltyHandlerArchTest {

  @ArchTest
  static final ArchRule penaltyHandlersDoNotDependOnCommitment =
      noClasses()
          .that()
          .resideInAPackage("com.waker.penalty.internal..")
          .and()
          .haveSimpleNameEndingWith("PenaltyHandler")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.waker.commitment..")
          .because("AD-2: penalty handlers must not depend on commitment types");

  /** Ledger entity/repo/impl must also stay free of commitment types (Story 3.1 / AD-2). */
  @ArchTest
  static final ArchRule penaltyModuleDoesNotDependOnCommitment =
      noClasses()
          .that()
          .resideInAPackage("com.waker.penalty..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.waker.commitment..")
          .because("AD-2: penalty module receives only ids/params — no commitment types");

  @ArchTest
  static final ArchRule penaltyHandlersDoNotDependOnUser =
      noClasses()
          .that()
          .resideInAPackage("com.waker.penalty.internal..")
          .and()
          .haveSimpleNameEndingWith("PenaltyHandler")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.waker.user..")
          .because("AD-3: penalty handlers must not depend on user types");
}
