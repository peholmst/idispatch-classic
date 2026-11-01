package net.pkhapps.idispatch.server.dispatch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.QrCode;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        var initial = destination == null ? Set.<ResourceId>of() : destination.resources();
        var choices = new ResourceChoices(service, initial);
        var resources = new MultiSelectComboBox<ResourceId>(getTranslation("management.resources"));
        resources.setRequiredIndicatorVisible(true);
        resources.setItemLabelGenerator(choices::callSign);
        // Set the items before binding, since setting them clears the value
        resources.setItems(choices.ids());
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
     * Adds a field for exactly one resource last, for a destination that has only one.
     */
    protected void addResourceField(DestinationManagementService service, @Nullable Destination destination) {
        var initial = destination == null ? Set.<ResourceId>of() : destination.resources();
        var choices = new ResourceChoices(service, initial);
        var resource = new ComboBox<ResourceId>(getTranslation("management.destination.resource"));
        resource.setRequiredIndicatorVisible(true);
        resource.setItemLabelGenerator(choices::callSign);
        // Set the items before binding, since setting them clears the value
        resource.setItems(choices.ids());
        getContent().add(resource);

        var touched = touchedSignal();
        var value = bind(resource, initial.stream().findFirst().orElse(null), touched);
        var error = value.map(chosen -> chosen == null
                ? getTranslation("management.destination.resource-required") : null);
        showError(resource, error, touched);
        resourcesValue = value.map(chosen -> chosen == null ? Set.of() : Set.of(chosen));
        resourcesError = error;
    }

    /**
     * The resources that can be chosen, which are the ones not deleted, with the call signs of those and of any deleted
     * ones that the destination still has.
     */
    private static final class ResourceChoices {

        private final Map<ResourceId, String> callSigns = new HashMap<>();
        private final List<ResourceId> ids;

        ResourceChoices(DestinationManagementService service, Set<ResourceId> chosen) {
            var applicable = service.findApplicableResources();
            applicable.forEach(resource -> callSigns.put(resource.id(), resource.callSign()));
            service.findResources(chosen).forEach(resource -> callSigns.put(resource.id(), resource.callSign()));
            ids = applicable.stream().map(ResourceListItem::id).toList();
        }

        List<ResourceId> ids() {
            return ids;
        }

        String callSign(ResourceId id) {
            return callSigns.getOrDefault(id, "");
        }
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

    /**
     * A mobile destination: one resource, and the pairing of the app below it. The key is shown as a QR code for the
     * pairing link only right after it was generated, when the destination was created or when the administrator
     * generates a new one, since only its hash is stored.
     */
    static final class Mobile extends DestinationForm {

        private final Signal<Optional<DestinationDetails>> draft;
        private final ValueSignal<Boolean> editable = new ValueSignal<>(true);

        /**
         * @param newKey the key just generated for the destination, to show, or {@code null} if there is none
         */
        Mobile(DestinationManagementService service, @Nullable MobileDestination destination,
               @Nullable MobileKey newKey) {
            super(destination);
            addResourceField(service, destination);
            getContent().add(destination == null ? new Paragraph(getTranslation(
                    "management.destination.pairing.after-saving"))
                    : createPairing(service, destination.id(), newKey));

            draft = Signal.computed(() -> commonFieldsValid()
                    ? Optional.of(new MobileDestinationDetails(descriptionValue(), localeValue(),
                    resourcesValue().iterator().next()))
                    : Optional.empty());
        }

        private Component createPairing(DestinationManagementService service, DestinationId id,
                                        @Nullable MobileKey newKey) {
            var key = new ValueSignal<@Nullable MobileKey>(newKey);
            var pairing = new Div();
            pairing.addClassName("mobile-pairing");
            Signal.effect(pairing, () -> {
                pairing.removeAll();
                pairing.add(new H4(getTranslation("management.destination.pairing")));
                var shown = key.get();
                if (shown == null) {
                    pairing.add(new Paragraph(getTranslation("management.destination.pairing.not-shown")));
                } else {
                    var link = pairingLink(shown);
                    var linkField = new TextField(getTranslation("management.destination.pairing.link"));
                    linkField.setValue(link);
                    linkField.setReadOnly(true);
                    linkField.setWidthFull();
                    pairing.add(new QrCode(link, getTranslation("management.destination.pairing.qr-code")),
                            new Paragraph(getTranslation("management.destination.pairing.scan-now")), linkField);
                }
                var newKeyButton = new Button(getTranslation("management.destination.pairing.new-key"),
                        event -> confirmNewKey(() -> key.set(service.replaceMobileKey(id))));
                newKeyButton.bindVisible(editable);
                pairing.add(newKeyButton);
            });
            return pairing;
        }

        private void confirmNewKey(Runnable onConfirm) {
            var dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("management.destination.pairing.new-key.header"));
            dialog.setText(getTranslation("management.destination.pairing.new-key.text"));
            dialog.setCancelable(true);
            dialog.setCancelText(getTranslation("common.cancel"));
            dialog.setConfirmText(getTranslation("management.destination.pairing.new-key.confirm"));
            dialog.setConfirmButtonTheme("error primary");
            dialog.addConfirmListener(event -> onConfirm.run());
            dialog.open();
        }

        /**
         * The link that pairs the app with the key: the app's address on this server, as the browser reaches it, with
         * the key in the fragment, which browsers never send to the server.
         */
        static String pairingLink(MobileKey key) {
            var request = VaadinServletRequest.getCurrent();
            var base = request == null ? ""
                    : ServletUriComponentsBuilder.fromContextPath(request.getHttpServletRequest()).toUriString();
            return base + "/mobile/#pair=" + key.value();
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            super.setReadOnly(readOnly);
            editable.set(!readOnly);
        }

        @Override
        protected Signal<Optional<DestinationDetails>> draft() {
            return draft;
        }
    }
}
