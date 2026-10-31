package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.ResourceManagementService;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceType;
import net.pkhapps.idispatch.server.ui.Formatters;

class ResourceForm extends ManagementForm<Resource> {

    private final TextField callSign;

    ResourceForm(ResourceManagementService service) {
        super(Resource.class);

        callSign = new TextField(getTranslation("management.call-sign"));

        var type = new ComboBox<ResourceType>(getTranslation("management.type"));
        type.setItemLabelGenerator(Formatters::formatResourceType);
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
