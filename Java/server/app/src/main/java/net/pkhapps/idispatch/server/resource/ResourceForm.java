package net.pkhapps.idispatch.server.resource;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Creates the {@link ResourceDetails} from a call sign and a type.
 */
class ResourceForm extends RecordForm<ResourceDetails> {

    private final TextField callSign;
    private final Signal<Optional<ResourceDetails>> draft;

    ResourceForm(ResourceManagementService service, @Nullable Resource resource) {
        callSign = new TextField(getTranslation("management.call-sign"));
        callSign.setRequiredIndicatorVisible(true);
        // Offers the types in use, but shows a deleted type that the resource already has
        var type = new ComboBox<ResourceTypeListItem>(getTranslation("management.type"));
        type.setRequiredIndicatorVisible(true);
        type.setItemLabelGenerator(ResourceFormatters::formatResourceType);
        // Set the items before binding, since setting them clears the value
        type.setItems(service.findApplicableTypes());
        getContent().add(callSign, type);

        var callSignTouched = touchedSignal();
        var typeTouched = touchedSignal();
        var callSignValue = bind(callSign, resource == null ? "" : resource.callSign().value(), callSignTouched);
        var typeValue = bind(type, resource == null ? null : service.findType(resource.typeId()).orElse(null),
                typeTouched);

        var callSignError = callSignValue.map(value -> CallSign.isValid(value)
                ? null : getTranslation("management.call-sign-required"));
        var typeError = typeValue.map(value -> value != null ? null : getTranslation("management.type-required"));
        showError(callSign, callSignError, callSignTouched);
        showError(type, typeError, typeTouched);

        draft = Signal.computed(() -> callSignError.get() == null && typeError.get() == null
                ? Optional.of(new ResourceDetails(new CallSign(callSignValue.get()), typeValue.get().id()))
                : Optional.empty());
    }

    @Override
    protected Signal<Optional<ResourceDetails>> draft() {
        return draft;
    }

    @Override
    public void focus() {
        callSign.focus();
    }
}
