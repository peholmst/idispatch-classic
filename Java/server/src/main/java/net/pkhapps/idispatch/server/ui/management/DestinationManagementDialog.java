package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.pkhapps.idispatch.server.boundary.DestinationManagementService;
import net.pkhapps.idispatch.server.entity.*;
import org.springframework.lang.Nullable;

public class DestinationManagementDialog extends AbstractManagementDialog<Destination> {

    protected DestinationManagementDialog(DestinationManagementService service) {
        super(service);
    }

    @Override
    public DestinationManagementService service() {
        return (DestinationManagementService) super.service();
    }

    @Override
    protected String getTitle(@Nullable Destination entity) {
        if (entity == null) {
            return "Add Destination";
        } else {
            if (entity instanceof SmsDestination) {
                return "Edit SMS Destination";
            } else if (entity instanceof RunboardDestination) {
                return "Edit Runboard Destination";
            } else {
                throw new IllegalStateException("Unknown destination type");
            }
        }
    }

    @Override
    protected Form configureForm(@Nullable Destination entity) {
        return new DestinationManagementForm(entity);
    }

    public class DestinationManagementForm implements Form {

        private final Component layout;
        private DestinationLayout destinationLayout;

        public DestinationManagementForm(@Nullable Destination entity) {
            if (entity == null) {
                var type = new RadioButtonGroup<DestinationLayout>();
                type.setItems(new RunboardDestinationLayout(new RunboardDestination()), new SmsDestinationLayout(new SmsDestination()));
                type.setItemLabelGenerator(DestinationLayout::getLabel);

                var layout = new VerticalLayout(type);
                type.addValueChangeListener(event -> {
                    if (event.getOldValue() != null) {
                        layout.remove(event.getOldValue());
                        destinationLayout = null;
                    }
                    if (event.getValue() != null) {
                        layout.add(event.getValue());
                        destinationLayout = event.getValue();
                    }
                });
                this.layout = layout;
            } else if (entity instanceof SmsDestination) {
                this.destinationLayout = new SmsDestinationLayout((SmsDestination) entity);
                this.layout = destinationLayout;
            } else if (entity instanceof RunboardDestination) {
                this.destinationLayout = new RunboardDestinationLayout((RunboardDestination) entity);
                this.layout = destinationLayout;
            } else {
                throw new IllegalStateException("Unknown destination type");
            }
        }

        @Override
        public Component getLayout() {
            return layout;
        }

        @Override
        public void save() throws ValidationFailedException {
            if (destinationLayout != null) {
                service().save(destinationLayout.getBinder().getBean());
            }
        }
    }

    private abstract static class DestinationLayout extends FormLayout {
        public abstract String getLabel();

        public abstract Binder<? extends Destination> getBinder();
    }

    private class SmsDestinationLayout extends DestinationLayout {

        private final Binder<SmsDestination> binder = new Binder<>();

        SmsDestinationLayout(SmsDestination entity) {
            binder.setBean(entity);
            var phoneNumbers = new TextArea("Phone numbers");
            final var info = new Span("Use commas or line breaks to separate numbers. Remember to add the +358 prefix!");
            add(new Div(phoneNumbers, info));
            binder.forField(phoneNumbers)
                    .withConverter(new PhoneNumbersToStringConverter())
                    .bind(SmsDestination::getPhoneNumbers, SmsDestination::setPhoneNumbers);

            var resources = new MultiSelectComboBox<Resource>("Resources");
            resources.setItems(service().findApplicableResources());
            resources.setItemLabelGenerator(Resource::getCallSign);
            add(resources);
            binder.bind(resources, SmsDestination::getResources, SmsDestination::setResources);
        }

        @Override
        public String getLabel() {
            return "SMS Destination";
        }

        @Override
        public Binder<? extends Destination> getBinder() {
            return binder;
        }
    }

    private class RunboardDestinationLayout extends DestinationLayout {

        private final Binder<RunboardDestination> binder = new Binder<>();

        RunboardDestinationLayout(RunboardDestination entity) {
            binder.setBean(entity);
            var runboardKey = new TextField("Runboard Key");
            add(runboardKey);
            binder.bind(runboardKey, RunboardDestination::getRunboardKey, RunboardDestination::setRunboardKey);

            var resources = new MultiSelectComboBox<Resource>("Resources");
            resources.setItems(service().findApplicableResources());
            resources.setItemLabelGenerator(Resource::getCallSign);
            add(resources);
            binder.bind(resources, RunboardDestination::getResources, RunboardDestination::setResources);
        }

        @Override
        public String getLabel() {
            return "Runboard Destination";
        }

        @Override
        public Binder<? extends Destination> getBinder() {
            return binder;
        }
    }
}
