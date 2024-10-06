package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.grader.api.bean.ServiceA;
import com.github.lernejo.korekto.grader.api.bean.ServiceB;
import com.github.lernejo.korekto.toolkit.GradingContext;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.lernejo.korekto.grader.api.ByteBuddys.GEN_PACKAGE;
import static com.github.lernejo.korekto.grader.api.ByteBuddys.saveToFileAndLoad;

@FunctionalInterface
public interface TypeSupplier<T> {

    static TypeSupplier<ErrorTesterType> errorTester() {
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
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "fr.lernejo.aop.Retry")).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName2, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "never_fails")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "fr.lernejo.aop.Retry")).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName3, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "fr.lernejo.aop.Retry")).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalArgumentException.class)).build())
                        .defineMethod(methodName4, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "fr.lernejo.aop.Retry")).define("maxTries", 2).build())
                        .defineMethod(methodName5, void.class, Modifier.PUBLIC)
                        .intercept(MethodCall.invoke(getMethod(errorTesterClass, "first_call_fails_second_succeed")))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "fr.lernejo.aop.Retry")).define("maxTries", 2).defineTypeArray("errorTypes", reload(cl, IllegalStateException.class)).build())
                        .make(),
                    directory,
                    cl);
                return Result.ok(new ErrorTesterType(
                    type,
                    new MethodDescription(methodName1, "always_fails"),
                    new MethodDescription(methodName2, "never_fails"),
                    new MethodDescription(methodName3, "first_call_fails_second_succeed(maxTries=2, errorTypes=IllegalArgumentException)"),
                    new MethodDescription(methodName4, "first_call_fails_second_succeed(maxTries=2)"),
                    new MethodDescription(methodName5, "first_call_fails_second_succeed(maxTries=2, errorTypes=IllegalStateException)"),
                    directory));
            } catch (RuntimeException e) {
                return Result.err("Missing annotation `fr.lernejo.aop.Retry`, or its structure is invalid (" + e.getMessage() + ")");
            }
        };
    }

    static TypeSupplier<SpringConfigType> springConfig() {
        return (c, cl) -> {
            Path directory = ByteBuddys.createTempClassDirectory(GradingContext.getRandomSource());
            Class<?> serviceA = reload(cl, ServiceA.class);
            Class<?> serviceB = reload(cl, ServiceB.class);
            try {
                Class<?> configurationType = saveToFileAndLoad(
                    ByteBuddys.BYTE_BUDDY
                        .subclass(Object.class)
                        .name(GEN_PACKAGE + "SpringTestConfig")
                        .annotateType(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "org.springframework.context.annotation.Configuration")).build())
                        .annotateType(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "org.springframework.context.annotation.EnableAspectJAutoProxy")).build())
                        .annotateType(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "org.springframework.context.annotation.ComponentScan")).defineArray("value", "com.github.lernejo.korekto.grader.api.bean").build())
                        .defineMethod("countingAspect", load(cl, "fr.lernejo.aop.CountingAspect"), Modifier.PUBLIC)
                        .intercept(FixedValue.value(newInstance(load(cl, "fr.lernejo.aop.CountingAspect"))))
                        .annotateMethod(AnnotationDescription.Builder.ofType(loadAnnotation(cl, "org.springframework.context.annotation.Bean")).build())
                        .make(),
                    directory,
                    cl);

                return Result.ok(null);
            } catch(RuntimeException e) {
                return Result.err("Missing `CountingAspect`, `InvocationTracker` or spring-aop dependency: " + e.getMessage());
            }
        };
    }

    static Object newInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    static Method getMethod(Class<?> className, String methodName) {
        try {
            return className.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static Class<?> load(ClassLoader cl, String className) {
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static Class<?> reload(ClassLoader cl, Class<?> clazz) {
        try {
            return cl.loadClass(clazz.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Class<? extends Annotation> loadAnnotation(ClassLoader cl, String name) {
        try {
            return (Class<? extends Annotation>) cl.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new NoAnnotationException(e);
        }
    }

    Result<T, String> supply(LaunchingContext context, ClassLoader tmpClassLoader);

    record ErrorTesterType(Class<?> type, MethodDescription method1, MethodDescription method2,
                           MethodDescription method3,
                           MethodDescription method4,
                           MethodDescription method5, Path directory) {
    }

    record SpringConfigType() {
    }

    record MethodDescription(String name, String methodDescription) {
    }
}
