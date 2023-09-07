package net.pkhapps.idispatch.server.ui.dws;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.Roles;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import org.springframework.context.event.ApplicationEventMulticaster;

@Route(value = "dws")
@RolesAllowed(Roles.ROLE_DISPATCHER)
@PageTitle("iDispatch")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@CssImport("./styles/dispatch-view-styles.css")
@CssImport(value = "./styles/dispatch-view-grid-styles.css", themeFor = "vaadin-grid")
public class DispatchView extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final AssignmentPanel assignmentPanel;
    private final AssignmentListPanel assignmentListPanel;
    private final ResourceListPanel resourceListPanel;

    public DispatchView(AssignmentService assignmentService,
                        ResourceStatusService resourceStatusService,
                        DispatchService dispatchService,
                        ApplicationEventMulticaster eventMulticaster) {
        this.assignmentService = assignmentService;
        assignmentPanel = new AssignmentPanel(assignmentService, resourceStatusService, dispatchService, eventMulticaster);
        assignmentListPanel = new AssignmentListPanel(assignmentService, eventMulticaster, assignmentPanel::setAssignmentId);
        resourceListPanel = new ResourceListPanel(resourceStatusService, eventMulticaster);
        addClassName("dispatch-view");
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.addClassName("toolbar");
        add(toolbar);

        var newAssignment = new Button("New Assignment", event -> newAssignment());
        newAssignment.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.add(newAssignment);

        var primarySplit = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
        primarySplit.setSizeFull();
        primarySplit.addThemeVariants(SplitLayoutVariant.LUMO_MINIMAL);
        add(primarySplit);


        primarySplit.setSplitterPosition(33);

        primarySplit.addToPrimary(assignmentPanel);
        var secondarySplit = new SplitLayout(SplitLayout.Orientation.VERTICAL);
        secondarySplit.setSizeFull();
        secondarySplit.addThemeVariants(SplitLayoutVariant.LUMO_MINIMAL);
        secondarySplit.addToPrimary(assignmentListPanel);
        secondarySplit.addToSecondary(resourceListPanel);
        primarySplit.addToSecondary(secondarySplit);
    }

    private void newAssignment() {
        var newId = assignmentService.openAssignment();
        assignmentListPanel.selectAssignmentById(newId);
    }
}
