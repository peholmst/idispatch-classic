package net.pkhapps.idispatch.server.entity;

public interface Deactivatable {

    String PROP_ACTIVE = "active";

    boolean isActive();

    void setActive(boolean active);
}
