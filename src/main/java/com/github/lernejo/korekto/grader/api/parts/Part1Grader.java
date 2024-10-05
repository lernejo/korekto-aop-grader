package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.MethodInvocationResult;
import com.github.lernejo.korekto.grader.api.Result;
import com.github.lernejo.korekto.grader.api.TypeSupplier;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record Part1Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    private static Result<Object, String> buildProxy(Method method, Object parameter) {
        try {
            return Result.ok(method.invoke(null, parameter));
        } catch (ReflectiveOperationException e) {
            return Result.err("Unable to create proxy: " + e.getMessage());
        }
    }

    private static Optional<Class<?>> getRetryableFactoryClass(ByteArrayClassLoader byteArrayClassLoader) {
        try {
            return Optional.of(byteArrayClassLoader.loadClass("fr.lernejo.aop.RetryableFactory"));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private static Optional<Method> getMethod(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(methodName)).findFirst();
    }

    private static MethodInvocationResult invoke(Object object, String methodName) {
        Optional<Method> potentialMethod = getMethod(object.getClass(), methodName);
        if (potentialMethod.isEmpty()) {
            return MethodInvocationResult.accessFailure(new NoSuchMethodException("No such method: " + methodName));
        }
        try {
            var result = potentialMethod.get().invoke(object);
            return MethodInvocationResult.ok(result);
        } catch (InvocationTargetException e) {
            return MethodInvocationResult.thrownFailure(e);
        } catch (IllegalAccessException e) {
            return MethodInvocationResult.accessFailure(e);
        }
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.hasCompilationFailed()) {
            return result(List.of("Not trying due to previous compilation failure"), 0.0D);
        }

        ClassLoader mavenClassLoader = context.getMavenMainClassloader();
        ByteArrayClassLoader byteArrayClassLoader = new ByteArrayClassLoader(mavenClassLoader, false, Map.of(), ByteArrayClassLoader.PersistenceHandler.MANIFEST);

        TypeSupplier typeSupplier = TypeSupplier.errorTester();

        Optional<TypeSupplier.Type> potentialType = typeSupplier.supply(context, byteArrayClassLoader);
        if (potentialType.isEmpty()) {
            return result(List.of("Missing annotation `fr.lernejo.aop.Retry`, or its structure is invalid"), 0.0D);
        }
        TypeSupplier.Type type = potentialType.get();
        Optional<Class<?>> potentialRetryableFactoryClass = getRetryableFactoryClass(byteArrayClassLoader);
        if (potentialRetryableFactoryClass.isEmpty()) {
            return result(List.of("Missing class `fr.lernejo.aop.RetryableFactory`"), 0.0D);
        }
        Optional<Method> potentialMethod = getMethod(potentialRetryableFactoryClass.get(), "buildRetryable");
        if (potentialMethod.isEmpty()) {
            return result(List.of("Missing method `fr.lernejo.aop.RetryableFactory#buildRetryable`"), 0.0D);
        }

        Supplier<Result<Object, String>> proxySupplier = () -> buildProxy(potentialMethod.get(), type.type());
        MutableResult result = MutableResult.maxGrade(maxGrade);

        invokeAndAssert(proxySupplier, type.method1(), result, Expect.FAILURE);
        if (result.grade().get() != 0) {
            invokeAndAssert(proxySupplier, type.method2(), result, Expect.SUCCESS);
            invokeAndAssert(proxySupplier, type.method3(), result, Expect.SUCCESS);
            invokeAndAssert(proxySupplier, type.method4(), result, Expect.SUCCESS);
            invokeAndAssert(proxySupplier, type.method5(), result, Expect.FAILURE);
        }

        return result(result.messages(), result.grade().get());
    }

    private void invokeAndAssert(Supplier<Result<Object, String>> proxySupplier, TypeSupplier.MethodDescription methodDesc, MutableResult result, Expect expect) {
        Result<Object, String> potentialProxy = proxySupplier.get();
        if (!potentialProxy.isOk()) {
            result.grade().set(0);
            result.messages().add(potentialProxy.err());
            return;
        }
        MethodInvocationResult invocationResult = invoke(potentialProxy.value(), methodDesc.name());
        if (invocationResult.status() == MethodInvocationResult.Status.ACCESS_FAILURE) {
            result.grade().getAndUpdate(g -> g - maxGrade / 5);
            result.messages().add("Error invoking method: " + methodDesc.name() + " (" + methodDesc.methodDescription() + "), " + invocationResult.accessError());
        } else if (expect == Expect.FAILURE && invocationResult.status() == MethodInvocationResult.Status.SUCCESS) {
            result.grade().getAndUpdate(g -> g - maxGrade / 5);
            result.messages().add("Unexpected success of method: " + methodDesc.name() + " (" + methodDesc.methodDescription() + ")");
        } else if (expect == Expect.SUCCESS && invocationResult.status() == MethodInvocationResult.Status.THROWN_FAILURE) {
            result.grade().getAndUpdate(g -> g - maxGrade / 5);
            result.messages().add("Unexpected failure of method: " + methodDesc.name() + " (" + methodDesc.methodDescription() + "), " + invocationResult.thrownError());
        }
    }

    enum Expect {
        SUCCESS,
        FAILURE,
        ;
    }
}
