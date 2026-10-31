package net.pkhapps.idispatch.server.dispatcher;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.assignment.AssignmentEvent;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

/**
 * The dispatcher's main view. Its state and actions are in a {@link DispatchViewModel}; the view lays out the panels
 * that bind to it, and tells it when the services' events say that assignments or resources have changed.
 */
@Route(value = "dispatcher", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_DISPATCHER)
public class DispatchView extends VerticalLayout implements HasDynamicTitle {

    private final DispatchViewModel viewModel;
    private final ApplicationEventMulticaster eventMulticaster;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<AssignmentEvent> assignmentListener;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusListener;

    public DispatchView(AssignmentService assignmentService,
                        ResourceStatusService resourceStatusService,
                        ResourceQueries resourceQueries,
                        DispatchService dispatchService,
                        ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
        viewModel = new DispatchViewModel(assignmentService, resourceStatusService, resourceQueries, dispatchService);
        assignmentListener = event -> ownerUpdater.update(viewModel::assignmentsChanged);
        resourceStatusListener = event -> ownerUpdater.update(viewModel::resourcesChanged);

        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var newAssignment = new Button(getTranslation("dispatch.new-assignment"),
                event -> viewModel.newAssignment());
        newAssignment.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.dispatcher")), newAssignment);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        var primarySplit = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
        primarySplit.setSizeFull();
        primarySplit.addThemeVariants(SplitLayoutVariant.SMALL);
        primarySplit.setSplitterPosition(33);
        add(primarySplit);

        primarySplit.addToPrimary(new AssignmentPanel(viewModel.assignment(), viewModel::changeResourceState));
        var secondarySplit = new SplitLayout(SplitLayout.Orientation.VERTICAL);
        secondarySplit.setSizeFull();
        secondarySplit.addThemeVariants(SplitLayoutVariant.SMALL);
        secondarySplit.addToPrimary(new AssignmentListPanel(viewModel));
        secondarySplit.addToSecondary(new ResourceListPanel(viewModel));
        primarySplit.addToSecondary(secondarySplit);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(assignmentListener);
        eventMulticaster.addApplicationListener(resourceStatusListener);
        viewModel.refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(assignmentListener);
        eventMulticaster.removeApplicationListener(resourceStatusListener);
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation("nav.dispatcher");
    }
}
