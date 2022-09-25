package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.boundary.ManagementService;
import net.pkhapps.idispatch.server.entity.AbstractEntity;

import static java.util.Objects.requireNonNull;

public abstract class AbstractManagementView<E extends AbstractEntity, S extends ManagementService<E>>
        extends VerticalLayout {

    private final S service;

    public AbstractManagementView(S service) {
        this.service = requireNonNull(service);

        var grid = new Grid<E>();
        configureGrid(grid);
        grid.setSizeFull();

        var add = new Button(VaadinIcon.PLUS_CIRCLE.create());
        var edit = new Button(VaadinIcon.EDIT.create());
        var refresh = new Button(VaadinIcon.REFRESH.create());

        setSizeFull();
        add(grid);

        var buttons = new HorizontalLayout(add, edit, refresh);
        add(buttons);

    }

    protected abstract void configureGrid(Grid<E> grid);

    protected S getService() {
        return service;
    }
}
