package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializablePredicate;
import net.pkhapps.idispatch.server.dispatch.MobilePhoneListItem;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.ui.audit.AuditTrail;

import java.util.List;
import java.util.Objects;

/**
 * Asks which unit to move a mobile phone to, telling which unit it is on now and who last changed it.
 */
class ChangeMobileUnitDialog extends Dialog {

    /**
     * @param units      the units the phone can be moved to
     * @param changeUnit moves the phone to the chosen unit, and tells whether it could
     */
    ChangeMobileUnitDialog(MobilePhoneListItem phone, String phoneName, List<ResourceListItem> units,
                           AuditLog auditLog, SerializablePredicate<ResourceId> changeUnit) {
        addClassName("change-mobile-unit-dialog");
        setHeaderTitle(getTranslation("mobile-phones.change-unit.title", phoneName));

        var current = new Span(getTranslation("mobile-phones.change-unit.current",
                Objects.requireNonNullElse(phone.callSign(), "?")));

        var unit = new ComboBox<ResourceListItem>(getTranslation("mobile-phones.change-unit.new-unit"));
        unit.setItems(units);
        unit.setItemLabelGenerator(ResourceListItem::callSign);
        unit.setRequiredIndicatorVisible(true);
        unit.setWidthFull();

        var trail = new AuditTrail(auditLog);
        trail.show(new AuditedEntity(AuditedEntity.Type.DESTINATION, phone.id().value()));

        var content = new VerticalLayout(current, unit, trail);
        content.setPadding(false);
        add(content);

        var save = new Button(getTranslation("common.save"), event -> {
            var chosen = unit.getValue();
            if (chosen == null) {
                return;
            }
            if (!changeUnit.test(chosen.id())) {
                var notification = new Notification(getTranslation("mobile-phones.change-unit.failed"));
                notification.addThemeVariants(NotificationVariant.WARNING);
                notification.setPosition(Notification.Position.MIDDLE);
                notification.setDuration(5000);
                notification.open();
            }
            close();
        });
        save.addThemeVariants(ButtonVariant.PRIMARY);
        save.setEnabled(false);
        unit.addValueChangeListener(event -> save.setEnabled(event.getValue() != null));
        var cancel = new Button(getTranslation("common.cancel"), event -> close());
        getFooter().add(cancel, save);

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }
}
