package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;

import java.util.Optional;

public interface HasNavbarContent {

    default Optional<Component> getNavbarContent() {
        return Optional.empty();
    }

    default String getTitle() {
        return "";
    }

}
