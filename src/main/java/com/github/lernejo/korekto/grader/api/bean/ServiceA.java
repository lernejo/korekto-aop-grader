package com.github.lernejo.korekto.grader.api.bean;

public class ServiceA {

    private final ServiceB serviceB;

    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }

    public void doStuff() {
        serviceB.doOtherStuff();
    }
}
