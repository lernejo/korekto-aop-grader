package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.GradingContext;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.implementation.MethodCall;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.lernejo.korekto.grader.api.ByteBuddys.GEN_PACKAGE;
import static com.github.lernejo.korekto.grader.api.ByteBuddys.saveToFileAndLoad;

@FunctionalInterface
public interface TypeSupplier {

    static TypeSupplier errorTester() {
        return (c, cl) -> {
            Path directory = ByteBuddys.createTempClassDirectory(GradingContext.getRandomSource());
            String methodName1 = c.newId();
            String methodName2 = c.newId();
            String methodName3 = c.newId();
            String methodName4 = c.newId();
            String methodName5 = c.newId();
            try {
                Class<?> errorTesterClass = reload(cl, ErrorTester.class);
                Class<?> type = saveToFileAndLoad(
                    ByteBuddys.BYTE_BUDDY
                        .subclass(errorTesterClass)
                        .name(GEN_PACKAGE + c.newTypeId())
                        .defineMethod(methodName1, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "always_fails")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadRetryAnnotation(cl)).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName2, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "never_fails")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadRetryAnnotation(cl)).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName3, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadRetryAnnotation(cl)).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName4, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadRetryAnnotation(cl)).define("maxTries", 2).build())
                        .defineMethod(methodName5, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadRetryAnnotation(cl)).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalStateException.class)).build())
                        .make(),
                    directory,
                    cl);
                return Optional.of(new Type(
                    type,
                    new MethodDescription(methodName1, "always_fails"),
                    new MethodDescription(methodName2, "never_fails"),
                    new MethodDescription(methodName3, "first_call_fails_second_succeed(maxTries=2, errorTypes=IllegalArgumentException)"),
                    new MethodDescription(methodName4, "first_call_fails_second_succeed(maxTries=2)"),
                    new MethodDescription(methodName5, "first_call_fails_second_succeed(maxTries=2, errorTypes=IllegalStateException)"),
                    directory));
            } catch (NoRetryAnnotation e) {
                return Optional.empty();
            }
        };
    }

    static Method getMethod(Class<?> className, String methodName) {
        try {
            return className.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static Class<?> reload(ClassLoader cl, Class<?> className) {
        try {
            return cl.loadClass(className.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Class<? extends Annotation> loadRetryAnnotation(ClassLoader cl) {
        try {
            return (Class<? extends Annotation>) cl.loadClass("fr.lernejo.aop.Retry");
        } catch (ClassNotFoundException e) {
            throw new NoRetryAnnotation(e);
        }
    }

    Optional<Type> supply(LaunchingContext context, ClassLoader tmpClassLoader);

    record Type(Class<?> type, MethodDescription method1, MethodDescription method2, MethodDescription method3,
                MethodDescription method4,
                MethodDescription method5, Path directory) {
    }

    record MethodDescription(String name, String methodDescription) {
    }
}
