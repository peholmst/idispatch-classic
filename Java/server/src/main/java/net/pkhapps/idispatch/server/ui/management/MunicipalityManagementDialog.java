package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.pkhapps.idispatch.server.boundary.MunicipalityManagementService;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.springframework.lang.Nullable;

public class MunicipalityManagementDialog extends AbstractManagementDialog<Municipality> {

    public MunicipalityManagementDialog(MunicipalityManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(@Nullable Municipality entity) {
        return entity == null ? "Add Municipality" : "Edit Municipality";
    }

    @Override
    protected Form configureForm(@Nullable Municipality entity) {
        return new SingleEntityForm() {
            @Override
            protected void initForm(Binder<Municipality> binder, FormLayout formLayout) {
                binder.setBean(entity == null ? new Municipality() : entity);

                var name = new TextField("Municipality name");
                formLayout.add(name);

                binder.bind(name, Municipality::getName, Municipality::setName);

                name.focus();
            }
        };
    }
}
