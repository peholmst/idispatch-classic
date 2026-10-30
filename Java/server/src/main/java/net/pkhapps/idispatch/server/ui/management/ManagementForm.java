package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.RequiredFieldConfigurator;
import jakarta.validation.constraints.NotBlank;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the forms shown in the detail area of the management views. Subclasses bind their fields by property
 * name, so that the bean validation constraints of the entity are checked next to each field.
 * <p>
 * The form is buffered: it reads the entity and writes the changes to a copy of it, so the entity shown in the grid
 * stays as it was if the user cancels or saving fails.
 */
abstract class ManagementForm<E extends AbstractEntity> extends Composite<FormLayout> {

    protected final BeanValidationBinder<E> binder;
    private @Nullable E formDataObject;

    protected ManagementForm(Class<E> entityType) {
        binder = new BeanValidationBinder<>(entityType);
        binder.setRequiredConfigurator(RequiredFieldConfigurator.DEFAULT
                .chain((annotation, binding) -> annotation.annotationType().equals(NotBlank.class)));

        var formLayout = getContent();
        formLayout.setAutoResponsive(true);
        formLayout.setExpandColumns(true);
        formLayout.setExpandFields(true);
    }

    public void setFormDataObject(E formDataObject) {
        this.formDataObject = requireNonNull(formDataObject);
        binder.readBean(formDataObject);
    }

    /**
     * Validates the form and returns a copy of the entity with the changes written to it, or an empty optional if
     * validation failed. The errors are then shown next to the fields.
     */
    public Optional<E> getFormDataObject() {
        var copy = copyOf(requireNonNull(formDataObject, "No form data object"));
        return binder.writeBeanIfValid(copy) ? Optional.of(copy) : Optional.empty();
    }

    public boolean hasChanges() {
        return binder.hasChanges();
    }

    public void setReadOnly(boolean readOnly) {
        binder.setReadOnly(readOnly);
    }

    public abstract void focus();

    @SuppressWarnings("unchecked")
    private E copyOf(E entity) {
        try {
            return (E) entity.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException("Could not copy " + entity, ex);
        }
    }
}
