package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.dispatch.MobilePhoneListItem;
import net.pkhapps.idispatch.server.dispatch.MobileUnitService;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * What the mobile phones view knows and can do: every mobile phone with the unit it is on, filtered by its description
 * or the unit's call sign, and moving a phone to another unit.
 * <p>
 * The list does not follow the services by itself. The view tells this ViewModel when a phone has been moved, through
 * {@link #phonesChanged()}, as the services' events arrive.
 */
class MobilePhonesViewModel {

    private final MobileUnitService service;

    private final ValueSignal<List<MobilePhoneListItem>> allPhones = new ValueSignal<>(List.of());
    private final ValueSignal<String> filter = new ValueSignal<>("");

    private final Signal<List<MobilePhoneListItem>> phones = Signal.computed(() -> {
        var text = filter.get().strip().toLowerCase(Locale.ROOT);
        return allPhones.get().stream()
                .filter(phone -> text.isEmpty() || contains(phone.description(), text)
                        || contains(phone.callSign(), text))
                .toList();
    });

    MobilePhonesViewModel(MobileUnitService service) {
        this.service = service;
    }

    // --- State ---

    /**
     * The phones that pass the filter, by description, then by call sign.
     */
    Signal<List<MobilePhoneListItem>> phones() {
        return phones;
    }

    /**
     * The text the listed phones' description or call sign must contain, ignoring case; empty lists every phone.
     */
    Signal<String> filter() {
        return filter.asReadonly();
    }

    /**
     * The units the phone can be moved to: every resource not deleted, except the one it is on, in call sign order.
     */
    List<ResourceListItem> unitsFor(MobilePhoneListItem phone) {
        return service.findUnits().stream().filter(unit -> !unit.id().equals(phone.unit())).toList();
    }

    // --- Commands ---

    /**
     * Loads the phones.
     */
    void refresh() {
        allPhones.set(service.findPhones());
    }

    /**
     * Loads the phones again, keeping the filter.
     */
    void phonesChanged() {
        refresh();
    }

    /**
     * Lists only the phones whose description or call sign contains the text, ignoring case.
     */
    void filterBy(String text) {
        filter.set(text);
    }

    /**
     * Moves the phone to the unit, and loads the phones again.
     *
     * @return whether the phone was moved; it is not if the phone or the unit has been deleted meanwhile
     */
    boolean changeUnit(MobilePhoneListItem phone, ResourceId unit) {
        try {
            service.changeUnit(phone.id(), unit);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        } finally {
            refresh();
        }
    }

    private static boolean contains(@Nullable String value, String text) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text);
    }
}
