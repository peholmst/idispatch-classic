package net.pkhapps.idispatch.server.dispatch;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the destination forms: a description and the language of the notifications first, then the fields
 * of the destination type, then the resources whose notifications the destination receives.
 */
abstract sealed class DestinationForm extends RecordForm<DestinationDetails> {

    private final TextField description;
    private final Signal<String> descriptionValue;
    private final Signal<@Nullable String> descriptionError;
    private final Signal<@Nullable Locale> localeValue;
    private final Signal<@Nullable String> localeError;
    private @Nullable Signal<Set<ResourceId>> resourcesValue;
    private @Nullable Signal<@Nullable String> resourcesError;

    protected DestinationForm(@Nullable Destination destination) {
        description = new TextField(getTranslation("management.description"));
        description.setHelperText(getTranslation("management.destination.description.helper"));
        var locale = new ComboBox<Locale>(getTranslation("management.destination.language"));
        locale.setRequiredIndicatorVisible(true);
        locale.setItems(List.of(Bilingual.FINNISH, Bilingual.SWEDISH));
        locale.setItemLabelGenerator(DestinationForm::formatLanguage);
        getContent().add(description, locale);

        var descriptionTouched = touchedSignal();
        var localeTouched = touchedSignal();
        descriptionValue = bind(description,
                destination == null || destination.description() == null ? "" : destination.description().value(),
                descriptionTouched);
        localeValue = bind(locale, destination == null ? Bilingual.FINNISH : destination.locale(), localeTouched);
        descriptionError = descriptionValue.map(value -> DestinationDescription.isValid(value)
                ? null : getTranslation("management.destination.description-too-long"));
        localeError = localeValue.map(value -> value != null
                ? null : getTranslation("management.destination.language-required"));
        showError(description, descriptionError, descriptionTouched);
        showError(locale, localeError, localeTouched);
    }

    /**
     * Adds the resources last, as the subclass's own fields go between them and the language.
     */
    protected void addResourcesField(DestinationManagementService service, @Nullable Destination destination) {
        // The call signs of the resources offered, and of any deleted ones that the destination still has
        var callSigns = new HashMap<ResourceId, String>();
        var applicable = service.findApplicableResources();
        applicable.forEach(resource -> callSigns.put(resource.id(), resource.callSign()));
        var initial = destination == null ? Set.<ResourceId>of() : destination.resources();
        service.findResources(initial).forEach(resource -> callSigns.put(resource.id(), resource.callSign()));

        var resources = new MultiSelectComboBox<ResourceId>(getTranslation("management.resources"));
        resources.setRequiredIndicatorVisible(true);
        resources.setItemLabelGenerator(id -> callSigns.getOrDefault(id, ""));
        // Set the items before binding, since setting them clears the value
        resources.setItems(applicable.stream().map(ResourceListItem::id).toList());
        getContent().add(resources);

        var touched = touchedSignal();
        var value = bind(resources, initial, touched);
        var error = value.map(chosen -> chosen.isEmpty()
                ? getTranslation("management.destination.resources-required") : null);
        showError(resources, error, touched);
        resourcesValue = value;
        resourcesError = error;
    }

    /**
     * Whether the fields that every destination has are valid. Reads the signals, so the draft follows them.
     */
    protected boolean commonFieldsValid() {
        return descriptionError.get() == null && localeError.get() == null
                && resourcesError != null && resourcesError.get() == null;
    }

    protected @Nullable DestinationDescription descriptionValue() {
        return DestinationDescription.of(descriptionValue.get());
    }

    protected Locale localeValue() {
        return requireNonNull(localeValue.get());
    }

    protected Set<ResourceId> resourcesValue() {
        return requireNonNull(resourcesValue).get();
    }

    static String formatLanguage(@Nullable Locale locale) {
        return locale == null ? "" : I18NProvider.translate("management.destination.language." + locale.getLanguage());
    }

    @Override
    public void focus() {
        description.focus();
    }

    static final class Sms extends DestinationForm {

        private final Signal<Optional<DestinationDetails>> draft;

        Sms(DestinationManagementService service, @Nullable SmsDestination destination) {
            super(destination);

            var phoneNumbers = new TextArea(getTranslation("management.destination.phone-numbers"));
            phoneNumbers.setHelperText(getTranslation("management.destination.phone-numbers.info"));
            phoneNumbers.setRequiredIndicatorVisible(true);
            getContent().add(phoneNumbers);
            var touched = touchedSignal();
            var text = bind(phoneNumbers, destination == null ? ""
                    : destination.phoneNumbers().stream().map(PhoneNumber::value).sorted()
                    .collect(Collectors.joining("\n")), touched);
            var numbers = text.map(PhoneNumber::parse);
            var error = numbers.map(parsed -> parsed.isEmpty()
                    ? getTranslation("management.destination.phone-numbers-required") : null);
            showError(phoneNumbers, error, touched);

            addResourcesField(service, destination);

            draft = Signal.computed(() -> commonFieldsValid() && error.get() == null
                    ? Optional.of(new SmsDestinationDetails(descriptionValue(), localeValue(), resourcesValue(),
                    numbers.get()))
                    : Optional.empty());
        }

        @Override
        protected Signal<Optional<DestinationDetails>> draft() {
            return draft;
        }
    }

    static final class Runboard extends DestinationForm {

        private final Signal<Optional<DestinationDetails>> draft;

        Runboard(DestinationManagementService service, @Nullable RunboardDestination destination) {
            super(destination);

            var runboardKey = new TextField(getTranslation("management.destination.runboard-key"));
            runboardKey.setRequiredIndicatorVisible(true);
            getContent().add(runboardKey);
            var touched = touchedSignal();
            var key = bind(runboardKey,
                    (destination == null ? RunboardKey.random() : destination.runboardKey()).value(), touched);
            var error = key.map(value -> RunboardKey.isValid(value)
                    ? null : getTranslation("management.destination.runboard-key-required"));
            showError(runboardKey, error, touched);

            addResourcesField(service, destination);

            draft = Signal.computed(() -> commonFieldsValid() && error.get() == null
                    ? Optional.of(new RunboardDestinationDetails(descriptionValue(), localeValue(), resourcesValue(),
                    new RunboardKey(key.get())))
                    : Optional.empty());
        }

        @Override
        protected Signal<Optional<DestinationDetails>> draft() {
            return draft;
        }
    }
}
