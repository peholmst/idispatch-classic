package net.pkhapps.idispatch.server.municipality;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.shared.BilingualText;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Creates the {@link MunicipalityDetails} from a name in Finnish and Swedish, of which one is enough.
 */
class MunicipalityForm extends RecordForm<MunicipalityDetails> {

    private final TextField nameFi;
    private final Signal<Optional<MunicipalityDetails>> draft;

    MunicipalityForm(@Nullable Municipality municipality) {
        nameFi = new TextField(getTranslation("management.municipality-name-fi"));
        var nameSv = new TextField(getTranslation("management.municipality-name-sv"));
        getContent().add(nameFi, nameSv);

        var nameTouched = touchedSignal();
        var finnish = bind(nameFi, municipality == null ? "" : municipality.name().finnish(), nameTouched);
        var swedish = bind(nameSv, municipality == null ? "" : municipality.name().swedish(), nameTouched);

        var nameError = Signal.computed(() -> BilingualText.isValid(finnish.get(), swedish.get())
                ? null : getTranslation("management.municipality-name-required"));
        showError(nameFi, nameError, nameTouched);
        showError(nameSv, nameError, nameTouched);

        draft = Signal.computed(() -> nameError.get() == null
                ? Optional.of(new MunicipalityDetails(new BilingualText(finnish.get(), swedish.get())))
                : Optional.empty());
    }

    @Override
    protected Signal<Optional<MunicipalityDetails>> draft() {
        return draft;
    }

    @Override
    public void focus() {
        nameFi.focus();
    }
}
