package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.dispatch.MobilePhoneListItem;
import net.pkhapps.idispatch.server.dispatch.MobileUnitChanged;
import net.pkhapps.idispatch.server.dispatch.MobileUnitService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.Objects;

import static net.pkhapps.idispatch.server.ui.SignalBindings.bindItems;

/**
 * Every unit's mobile phone with the unit it is on, for the dispatcher to move a phone to another unit, as when the
 * officer who carries it changes vehicles. Clicking a phone's unit asks which unit to move it to. The list follows the
 * moves of every dispatcher through server push.
 */
@Route(value = "mobile-phones", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_DISPATCHER)
public class MobilePhonesView extends VerticalLayout implements HasDynamicTitle {

    private final MobilePhonesViewModel viewModel;
    private final AuditLog auditLog;
    private final ApplicationEventMulticaster eventMulticaster;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<MobileUnitChanged> mobileUnitListener;

    public MobilePhonesView(MobileUnitService mobileUnitService, AuditLog auditLog,
                            ApplicationEventMulticaster eventMulticaster) {
        this.auditLog = auditLog;
        this.eventMulticaster = eventMulticaster;
        viewModel = new MobilePhonesViewModel(mobileUnitService);
        mobileUnitListener = event -> ownerUpdater.update(viewModel::phonesChanged);

        addClassName("mobile-phones-view");
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var filterField = new TextField();
        filterField.setPlaceholder(getTranslation("mobile-phones.filter"));
        filterField.setAriaLabel(getTranslation("mobile-phones.filter-label"));
        filterField.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidth(20, Unit.EM);
        filterField.bindValue(viewModel.filter(), viewModel::filterBy);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.mobile-phones")), filterField);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.setWrap(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        var grid = new Grid<MobilePhoneListItem>();
        grid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAriaLabel(getTranslation("mobile-phones.all"));
        grid.setEmptyStateText(getTranslation("mobile-phones.none"));
        grid.addColumn(this::phoneName).setHeader(getTranslation("mobile-phones.phone"));
        grid.addColumn(new ComponentRenderer<>(this::createUnitButton)).setHeader(getTranslation("mobile-phones.unit"));
        bindItems(grid, viewModel.phones()).setIdentifierProvider(MobilePhoneListItem::id);
        addAndExpand(grid);
    }

    /**
     * The description that tells the phone apart, or the unit's call sign for a phone without one.
     */
    private String phoneName(MobilePhoneListItem phone) {
        return phone.description() != null ? phone.description()
                : getTranslation("mobile-phones.phone-of", Objects.requireNonNullElse(phone.callSign(), "?"));
    }

    private Button createUnitButton(MobilePhoneListItem phone) {
        var button = new Button(Objects.requireNonNullElse(phone.callSign(), "?"));
        button.addThemeVariants(ButtonVariant.SMALL);
        button.setTooltipText(getTranslation("mobile-phones.change-unit"));
        button.addClickListener(event -> new ChangeMobileUnitDialog(phone, phoneName(phone),
                viewModel.unitsFor(phone), auditLog, unit -> viewModel.changeUnit(phone, unit)).open());
        return button;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(mobileUnitListener);
        viewModel.refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(mobileUnitListener);
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation("nav.mobile-phones");
    }
}
