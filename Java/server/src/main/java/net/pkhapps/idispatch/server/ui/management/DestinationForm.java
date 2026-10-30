package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.DestinationManagementService;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.RunboardDestination;
import net.pkhapps.idispatch.server.entity.SmsDestination;

/**
 * Base class for the destination forms: a description first, then the fields of the destination type, then the
 * resources whose notifications the destination receives.
 */
abstract class DestinationForm<D extends Destination> extends ManagementForm<D> {

    private final TextField description;

    protected DestinationForm(Class<D> entityType) {
        super(entityType);

        description = new TextField(getTranslation("management.description"));
        description.setHelperText(getTranslation("management.destination.description.helper"));
        getContent().add(description);

        binder.forField(description)
                .withNullRepresentation("")
                .bind(Destination.PROP_DESCRIPTION);
    }

    protected void addResourcesField(DestinationManagementService service) {
        var resources = new MultiSelectComboBox<Resource>(getTranslation("management.resources"));
        resources.setItems(service.findApplicableResources());
        resources.setItemLabelGenerator(Resource::getCallSign);
        getContent().add(resources);

        binder.forField(resources).bind(Destination.PROP_RESOURCES);
    }

    @Override
    public void focus() {
        description.focus();
    }

    static class Sms extends DestinationForm<SmsDestination> {

        Sms(DestinationManagementService service) {
            super(SmsDestination.class);

            var phoneNumbers = new TextArea(getTranslation("management.destination.phone-numbers"));
            phoneNumbers.setHelperText(getTranslation("management.destination.phone-numbers.info"));
            getContent().add(phoneNumbers);

            binder.forField(phoneNumbers)
                    .withConverter(new PhoneNumbersToStringConverter())
                    .bind(SmsDestination.PROP_PHONE_NUMBERS);

            addResourcesField(service);
        }
    }

    static class Runboard extends DestinationForm<RunboardDestination> {

        Runboard(DestinationManagementService service) {
            super(RunboardDestination.class);

            var runboardKey = new TextField(getTranslation("management.destination.runboard-key"));
            getContent().add(runboardKey);

            binder.forField(runboardKey).bind(RunboardDestination.PROP_RUNBOARD_KEY);

            addResourcesField(service);
        }
    }
}
