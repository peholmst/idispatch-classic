package net.pkhapps.idispatch.server.municipality;

import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.ui.management.ManagementForm;

class MunicipalityForm extends ManagementForm<Municipality> {

    private final TextField nameFi;

    MunicipalityForm() {
        super(Municipality.class);

        nameFi = new TextField(getTranslation("management.municipality-name-fi"));
        var nameSv = new TextField(getTranslation("management.municipality-name-sv"));
        getContent().add(nameFi, nameSv);

        bindBilingual(nameFi, Municipality.PROP_NAME_FI, nameSv, Municipality.PROP_NAME_SV,
                "management.municipality-name-required");
    }

    @Override
    public void focus() {
        nameFi.focus();
    }
}
