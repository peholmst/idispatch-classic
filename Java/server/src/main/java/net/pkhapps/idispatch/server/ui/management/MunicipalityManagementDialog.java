package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.MunicipalityManagementService;
import net.pkhapps.idispatch.server.entity.Municipality;

public class MunicipalityManagementDialog extends AbstractManagementDialog<Municipality> {

    public MunicipalityManagementDialog(MunicipalityManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(Municipality entity) {
        return entity.isNew() ? "Add Municipality" : "Edit Municipality";
    }

    @Override
    protected void configureForm(FormLayout form) {
        var name = new TextField("Municipality name");
        form.add(name);

        getBinder().bind(name, Municipality::getName, Municipality::setName);

        name.focus();
    }
}
