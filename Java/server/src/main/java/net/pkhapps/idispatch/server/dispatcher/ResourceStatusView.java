package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.assignment.AssignmentEvent;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.Objects;

/**
 * The state of every resource at a glance: a card per resource, colored by its state, with its call sign and the
 * assignment it is on, in call sign order. The dispatcher filters the cards by call sign and by state. The cards
 * follow the services' events, so a change made anywhere shows up right away through server push.
 */
@Route(value = "resource-status", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_DISPATCHER)
public class ResourceStatusView extends VerticalLayout implements HasDynamicTitle {

    private static final String CARD_WIDTH = "14rem";

    private final ResourceStatusViewModel viewModel;
    private final ApplicationEventMulticaster eventMulticaster;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusListener;
    // The cards show the assignments, which can change while the resources on them stay as they are
    private final ApplicationListener<AssignmentEvent> assignmentListener;

    public ResourceStatusView(ResourceStatusService resourceStatusService,
                              ResourceQueries resourceQueries,
                              AssignmentService assignmentService,
                              ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
        viewModel = new ResourceStatusViewModel(resourceStatusService, resourceQueries, assignmentService);
        resourceStatusListener = event -> ownerUpdater.update(viewModel::resourcesChanged);
        assignmentListener = event -> ownerUpdater.update(viewModel::resourcesChanged);

        addClassName("resource-status-view");
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var callSignFilter = new TextField();
        callSignFilter.setPlaceholder(getTranslation("dispatch.filter-resources"));
        callSignFilter.setAriaLabel(getTranslation("dispatch.filter-resources-label"));
        callSignFilter.setPrefixComponent(VaadinIcon.SEARCH.create());
        callSignFilter.setClearButtonVisible(true);
        callSignFilter.setValueChangeMode(ValueChangeMode.LAZY);
        callSignFilter.bindValue(viewModel.callSignFilter(), viewModel::filterByCallSign);

        var stateFilter = new MultiSelectComboBox<ResourceState>();
        stateFilter.setItems(ResourceState.values());
        stateFilter.setItemLabelGenerator(ResourceFormatters::formatResourceState);
        stateFilter.setPlaceholder(getTranslation("resource-status.filter-states"));
        stateFilter.setAriaLabel(getTranslation("resource-status.filter-states-label"));
        stateFilter.setClearButtonVisible(true);
        stateFilter.addClassName("resource-status-state-filter");
        stateFilter.bindValue(viewModel.stateFilter(), viewModel::filterByStates);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.resource-status")), callSignFilter,
                stateFilter);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.setWrap(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        // Tiles of the same width that wrap onto as many rows as they need, with space between them
        var cards = new HorizontalLayout();
        cards.addClassName("resource-status-cards");
        cards.setWrap(true);
        cards.setSpacing(true);
        cards.setPadding(false);
        cards.setWidthFull();
        cards.setDefaultVerticalComponentAlignment(Alignment.STRETCH);
        cards.getElement().setAttribute("role", "list");
        cards.getElement().setAttribute("aria-label", getTranslation("resource-status.cards"));
        Signal.effect(cards, () -> {
            cards.removeAll();
            viewModel.resources().get().forEach(row -> cards.add(createCard(row)));
        });

        var noResources = new Span(getTranslation("resource-status.no-resources"));
        noResources.addClassName("resource-status-empty");
        noResources.bindVisible(Signal.computed(() -> viewModel.resources().get().isEmpty()));

        var content = new Div(cards, noResources);
        content.addClassName("resource-status-content");
        addAndExpand(content);
    }

    private Component createCard(ResourceRow row) {
        var state = row.status().state();
        var card = new Card();
        card.addClassNames("resource-status-card", ResourceFormatters.resourceStateClassName(state));
        card.setAriaRole("listitem");
        card.setWidth(CARD_WIDTH);
        card.setTitle(Objects.requireNonNullElse(row.callSign(), "?"), 3);
        card.setSubtitle(ResourceFormatters.formatResourceState(state));
        // A resource that is available again or out of service still refers to its last assignment, but is not on it
        if (row.status().isAssigned() && row.assignment() != null) {
            var assignment = new Span(AssignmentFormatters.formatAssignment(row.assignment()));
            assignment.addClassName("resource-status-assignment");
            card.add(assignment);
        }
        return card;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(resourceStatusListener);
        eventMulticaster.addApplicationListener(assignmentListener);
        viewModel.refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(resourceStatusListener);
        eventMulticaster.removeApplicationListener(assignmentListener);
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation("nav.resource-status");
    }
}
