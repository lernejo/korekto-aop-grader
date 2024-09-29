package com.github.lernejo.korekto.grader.api;

public record Result<VALUE, ERR>(VALUE value, ERR err) {
    public static <VALUE, ERR> Result<VALUE, ERR> ok(VALUE value) {
        return new Result<>(value, null);
    }

    public static <VALUE, ERR> Result<VALUE, ERR> err(ERR err) {
        return new Result<>(null, err);
    }

    public boolean isOk() {
        return value != null && err == null;
    }
}
