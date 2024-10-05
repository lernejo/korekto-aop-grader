package com.github.lernejo.korekto.grader.api;

import java.lang.reflect.InvocationTargetException;

public record MethodInvocationResult(Status status, Object value, Exception accessError, Throwable thrownError) {
    public static MethodInvocationResult accessFailure(ReflectiveOperationException e) {
        return new MethodInvocationResult(Status.ACCESS_FAILURE, null, e, null);
    }

    public static MethodInvocationResult thrownFailure(InvocationTargetException e) {
        return new MethodInvocationResult(Status.THROWN_FAILURE, null, null, e.getCause());
    }

    public static MethodInvocationResult ok(Object value) {
        return new MethodInvocationResult(Status.SUCCESS, value, null, null);
    }

    public enum Status {
        SUCCESS,
        ACCESS_FAILURE,
        THROWN_FAILURE,
        ;
    }
}
