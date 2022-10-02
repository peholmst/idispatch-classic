package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.pkhapps.idispatch.server.boundary.AssignmentTypeManagementService;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import org.springframework.lang.Nullable;

public class AssignmentTypeManagementDialog extends AbstractManagementDialog<AssignmentType> {

    public AssignmentTypeManagementDialog(AssignmentTypeManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(@Nullable AssignmentType entity) {
        return entity == null ? "Add Assignment Type" : "Edit Assignment Type";
    }

    @Override
    protected Form configureForm(@Nullable AssignmentType entity) {
        return new SingleEntityForm() {
            @Override
            protected void initForm(Binder<AssignmentType> binder, FormLayout formLayout) {
                binder.setBean(entity == null ? new AssignmentType() : entity);

                var code = new TextField("Code");
                formLayout.add(code);

                var description = new TextField("Description");
                formLayout.add(description);

                binder.bind(code, AssignmentType::getCode, AssignmentType::setCode);
                binder.bind(description, AssignmentType::getDescription, AssignmentType::setDescription);

                code.focus();

            }
        };
    }
}
