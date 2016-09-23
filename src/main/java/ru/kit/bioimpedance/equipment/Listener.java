package ru.kit.bioimpedance.equipment;

@FunctionalInterface
public interface Listener {

    void callback(Object... object);
}
