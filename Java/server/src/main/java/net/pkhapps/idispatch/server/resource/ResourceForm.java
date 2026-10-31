package net.pkhapps.idispatch.server.resource;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.ui.management.ManagementForm;

class ResourceForm extends ManagementForm<Resource> {

    private final TextField callSign;

    ResourceForm(ResourceManagementService service) {
        super(Resource.class);

        callSign = new TextField(getTranslation("management.call-sign"));

        var type = new ComboBox<ResourceType>(getTranslation("management.type"));
        type.setItemLabelGenerator(ResourceFormatters::formatResourceType);
        type.setItems(service.findApplicableTypes());

        getContent().add(callSign, type);

        binder.forField(callSign).bind(Resource.PROP_CALL_SIGN);
        binder.forField(type).bind(Resource.PROP_TYPE);
    }

    @Override
    public void focus() {
        callSign.focus();
    }
}
