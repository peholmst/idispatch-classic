package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * The drawer toggle and the title of a view, for the toolbar at the top of the view. {@link RootLayout} has no navbar,
 * so every view shows its own title and toolbar.
 */
public class ViewTitle extends Composite<HorizontalLayout> {

    public ViewTitle(String title) {
        addClassName("view-title");
        getContent().add(new DrawerToggle(), new H2(title));
        getContent().setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    }
}
