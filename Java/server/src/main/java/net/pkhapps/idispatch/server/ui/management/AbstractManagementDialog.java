package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableRunnable;
import net.pkhapps.idispatch.server.boundary.ManagementService;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@CssImport("./styles/management-dialog-styles.css")
public abstract class AbstractManagementDialog<E extends AbstractEntity> extends Dialog {

    private final ManagementService<E> service;
    private final UnorderedList validationErrors;
    private SerializableRunnable onClose;
    private Form form;

    protected AbstractManagementDialog(ManagementService<E> service) {
        this.service = requireNonNull(service);

        validationErrors = new UnorderedList();
        validationErrors.addClassName("validation-errors"); // TODO Make styles for this
        add(validationErrors);

        var save = new Button("Save", event -> saveAndClose());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);

        var cancel = new Button("Cancel", event -> close());

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        getFooter().add(save, cancel);

        addDialogCloseActionListener(event -> close());
    }

    @Override
    public void close() {
        if (onClose != null) {
            onClose.run();
        }
        super.close();
    }

    private void saveAndClose() {
        try {
            form.save();
            close();
        } catch (ValidationFailedException ex) {
            validationErrors.setVisible(true);
            ex.getViolationMessages(Locale.getDefault()).forEach(message -> validationErrors.add(new ListItem(message)));
        } catch (DataIntegrityViolationException ex) {
            var notification = new Notification("A data integrity violation has been detected. You may have entered something that already exists. Check the form and try again.");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.setDuration(3000);
            notification.open();
        }
    }

    public void openNew(@Nullable SerializableRunnable onClose) {
        open(null, onClose);
    }

    public void open(@Nullable E entity, @Nullable SerializableRunnable onClose) {
        this.onClose = onClose;
        setEntity(entity);
        open();
    }

    protected void setEntity(@Nullable E entity) {
        setHeaderTitle(getTitle(entity));
        validationErrors.setVisible(false);
        validationErrors.removeAll();
        if (form != null) {
            remove(form.getLayout());
        }
        form = configureForm(entity);
        addComponentAsFirst(form.getLayout());
    }

    protected abstract String getTitle(@Nullable E entity);

    protected abstract Form configureForm(@Nullable E entity);

    public ManagementService<E> service() {
        return service;
    }

    public interface Form {
        Component getLayout();

        void save() throws ValidationFailedException;
    }

    public abstract class SingleEntityForm implements Form {
        private final FormLayout formLayout;
        private final Binder<E> binder;

        public SingleEntityForm() {
            binder = new Binder<>();
            formLayout = new FormLayout();
            initForm(binder, formLayout);
        }

        protected abstract void initForm(Binder<E> binder, FormLayout formLayout);

        @Override
        public final Component getLayout() {
            return formLayout;
        }

        @Override
        public void save() throws ValidationFailedException {
            service().save(binder.getBean());
        }
    }
}
