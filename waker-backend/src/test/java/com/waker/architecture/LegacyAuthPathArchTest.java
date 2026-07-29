package com.waker.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guard against legacy {@code /auth/...} path regressions (Story 1.6 public-surface audit). New
 * APIs must stay under {@code /api/v1}.
 */
@AnalyzeClasses(packages = "com.waker", importOptions = ImportOption.DoNotIncludeTests.class)
class LegacyAuthPathArchTest {

  @ArchTest
  static final ArchRule noLegacyAuthPathMappings =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should(notMapUnderLegacyAuth())
          .because("legacy /auth/ paths must not exist in waker-backend (AD-9 / FR25)")
          .allowEmptyShould(true);

  private static ArchCondition<JavaClass> notMapUnderLegacyAuth() {
    return new ArchCondition<>("not map under /auth/") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        Stream<String> paths =
            Stream.concat(classLevelPaths(javaClass), methodLevelPaths(javaClass));
        boolean violated = paths.anyMatch(LegacyAuthPathArchTest::isLegacyAuth);
        if (violated) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " must not map under /auth/"));
        } else {
          events.add(
              SimpleConditionEvent.satisfied(
                  javaClass, javaClass.getName() + " has no legacy /auth/ mapping"));
        }
      }
    };
  }

  private static boolean isLegacyAuth(String path) {
    return path.equals("/auth") || path.startsWith("/auth/");
  }

  private static Stream<String> classLevelPaths(JavaClass javaClass) {
    return mappingPaths(javaClass.getAnnotations());
  }

  private static Stream<String> methodLevelPaths(JavaClass javaClass) {
    return javaClass.getMethods().stream().flatMap(method -> mappingPaths(method.getAnnotations()));
  }

  private static Stream<String> mappingPaths(Collection<? extends JavaAnnotation<?>> annotations) {
    return annotations.stream()
        .filter(
            annotation ->
                annotation.getRawType().isAssignableTo(RequestMapping.class)
                    || annotation
                        .getRawType()
                        .getName()
                        .startsWith("org.springframework.web.bind.annotation."))
        .flatMap(
            annotation -> {
              Optional<Object> value = annotation.get("value");
              Optional<Object> path = annotation.get("path");
              return Stream.concat(asStringStream(value), asStringStream(path));
            });
  }

  private static Stream<String> asStringStream(Optional<Object> attribute) {
    return attribute.stream()
        .flatMap(
            value -> {
              if (value instanceof String[] strings) {
                return Arrays.stream(strings);
              }
              if (value instanceof String string) {
                return Stream.of(string);
              }
              return Stream.empty();
            });
  }
}
