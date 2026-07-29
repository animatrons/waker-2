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
 * AD-12: business HTTP APIs mount under {@code /api/v1}. Controllers with HTTP mappings must use
 * that prefix. Actuator stays under {@code /actuator} via Spring Boot, outside application
 * controllers.
 */
@AnalyzeClasses(packages = "com.waker", importOptions = ImportOption.DoNotIncludeTests.class)
class ApiVersioningArchTest {

  @ArchTest
  static final ArchRule restControllersUseApiV1Prefix =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should(mapUnderApiV1WhenMapped())
          .because("AD-12: controllers must use @RequestMapping under /api/v1/...")
          .allowEmptyShould(true); // no RestControllers yet at Story 1.3

  private static ArchCondition<JavaClass> mapUnderApiV1WhenMapped() {
    return new ArchCondition<>("be mapped under /api/v1 when HTTP mappings exist") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        Stream<String> paths =
            Stream.concat(classLevelPaths(javaClass), methodLevelPaths(javaClass));
        var pathList = paths.toList();
        if (pathList.isEmpty()) {
          events.add(
              SimpleConditionEvent.satisfied(
                  javaClass, javaClass.getName() + " has no HTTP mappings yet"));
          return;
        }
        boolean ok = pathList.stream().anyMatch(ApiVersioningArchTest::isApiV1);
        if (ok) {
          events.add(
              SimpleConditionEvent.satisfied(javaClass, javaClass.getName() + " uses /api/v1"));
        } else {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " must map under /api/v1"));
        }
      }
    };
  }

  private static boolean isApiV1(String path) {
    return path.equals("/api/v1") || path.startsWith("/api/v1/");
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
