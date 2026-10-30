package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.entity.Municipality;

class MunicipalityForm extends ManagementForm<Municipality> {

    private final TextField name;

    MunicipalityForm() {
        super(Municipality.class);

        name = new TextField(getTranslation("management.municipality-name"));
        getContent().add(name);

        binder.forField(name).bind(Municipality.PROP_NAME);
    }

    @Override
    public void focus() {
        name.focus();
    }
}
