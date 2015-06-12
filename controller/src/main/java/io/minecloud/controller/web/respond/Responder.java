package io.minecloud.controller.web.respond;

public interface Responder<T> {
    void respond(T type);
}
