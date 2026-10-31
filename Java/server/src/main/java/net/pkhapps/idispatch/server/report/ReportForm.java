package net.pkhapps.idispatch.server.report;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates the {@link ReportDraft} from what happened on scene and the answers of each responding unit. A count must
 * not be negative, and the injured and deceased must not add up to more than the people involved; the counts share one
 * touched signal, since the rule between them shows its error as soon as any of them changes.
 */
class ReportForm extends RecordForm<ReportDraft> {

    private final TextArea situationDevelopment;
    private final Signal<@Nullable String> casualtiesError;
    private final Map<ResourceId, Details> unitDetails = new LinkedHashMap<>();
    private final Map<ResourceId, Signal<Boolean>> unitValid = new HashMap<>();
    private final Signal<Optional<ReportDraft>> draft;

    /**
     * @param openedUnits the units to show opened; every other unit starts closed
     */
    ReportForm(AssignmentReport report, List<ResourceListItem> respondingResources,
               Set<ResourceId> openedUnits) {
        getContent().setMaxColumns(1);

        var sceneForm = new FormLayout();
        sceneForm.setWidthFull();
        sceneForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2), new FormLayout.ResponsiveStep("600px", 4));

        var scene = report.scene();
        situationDevelopment = createTextArea(getTranslation("report.situation-development"));
        sceneForm.add(situationDevelopment, 4);
        var situationTouched = touchedSignal();
        var situation = bind(situationDevelopment, scene.situationDevelopment().value(), situationTouched);
        var situationError = situation.map(value -> ReportText.isValid(value)
                ? null : getTranslation("report.situation-development-too-long"));
        showError(situationDevelopment, situationError, situationTouched);

        var countsTouched = touchedSignal();
        var peopleInvolvedField = createCountField(getTranslation("report.people-involved"));
        var lightlyInjuredField = createCountField(getTranslation("report.lightly-injured"));
        var seriouslyInjuredField = createCountField(getTranslation("report.seriously-injured"));
        var deceasedField = createCountField(getTranslation("report.deceased"));
        sceneForm.add(peopleInvolvedField, lightlyInjuredField, seriouslyInjuredField, deceasedField);
        var peopleInvolved = bind(peopleInvolvedField, scene.peopleInvolved(), countsTouched);
        var lightlyInjured = bind(lightlyInjuredField, scene.lightlyInjured(), countsTouched);
        var seriouslyInjured = bind(seriouslyInjuredField, scene.seriouslyInjured(), countsTouched);
        var deceased = bind(deceasedField, scene.deceased(), countsTouched);
        casualtiesError = Signal.computed(() -> SceneDetails.casualtiesWithinPeopleInvolved(peopleInvolved.get(),
                lightlyInjured.get(), seriouslyInjured.get(), deceased.get())
                ? null : getTranslation("report.casualties-too-many"));
        var peopleInvolvedError = countError(peopleInvolved, casualtiesError);
        var lightlyInjuredError = countError(lightlyInjured, null);
        var seriouslyInjuredError = countError(seriouslyInjured, null);
        var deceasedError = countError(deceased, null);
        showError(peopleInvolvedField, peopleInvolvedError, countsTouched);
        showError(lightlyInjuredField, lightlyInjuredError, countsTouched);
        showError(seriouslyInjuredField, seriouslyInjuredError, countsTouched);
        showError(deceasedField, deceasedError, countsTouched);

        var sceneLastEdit = createLastEditSpan();
        sceneLastEdit.setText(formatLastEdit(report.lastEdit()));

        var units = new VerticalLayout();
        units.setPadding(false);
        var unitAnswers = new LinkedHashMap<ResourceId, Signal<UnitAnswers>>();
        if (respondingResources.isEmpty()) {
            units.add(new Span(getTranslation("report.no-responding-units")));
        }
        for (var resource : respondingResources) {
            var unit = report.unit(resource.id());
            var situationOnArrival = createTextArea(getTranslation("report.unit.situation-on-arrival"));
            var actionsTaken = createTextArea(getTranslation("report.unit.actions-taken"));
            var situationOnArrivalTouched = touchedSignal();
            var actionsTakenTouched = touchedSignal();
            var situationOnArrivalValue = bind(situationOnArrival, unit.answers().situationOnArrival().value(),
                    situationOnArrivalTouched);
            var actionsTakenValue = bind(actionsTaken, unit.answers().actionsTaken().value(), actionsTakenTouched);
            var situationOnArrivalError = situationOnArrivalValue.map(value -> ReportText.isValid(value)
                    ? null : getTranslation("report.unit.situation-on-arrival-too-long"));
            var actionsTakenError = actionsTakenValue.map(value -> ReportText.isValid(value)
                    ? null : getTranslation("report.unit.actions-taken-too-long"));
            showError(situationOnArrival, situationOnArrivalError, situationOnArrivalTouched);
            showError(actionsTaken, actionsTakenError, actionsTakenTouched);
            unitValid.put(resource.id(), Signal.computed(() -> situationOnArrivalError.get() == null
                    && actionsTakenError.get() == null));
            unitAnswers.put(resource.id(), Signal.computed(() -> unitValid.get(resource.id()).get()
                    ? new UnitAnswers(new ReportText(situationOnArrivalValue.get()),
                    new ReportText(actionsTakenValue.get()))
                    : UnitAnswers.EMPTY));

            var status = Signal.computed(() -> UnitReportStatus.of(situationOnArrivalValue.get(),
                    actionsTakenValue.get()));
            var unitForm = new VerticalLayout(situationOnArrival, actionsTaken);
            unitForm.setPadding(false);
            unitForm.setSpacing(false);
            var details = new Details(createUnitSummary(resource, unit, status), unitForm);
            details.setWidthFull();
            // A writer usually works on their own unit only, so every unit starts closed and stays out of the way
            details.setOpened(openedUnits.contains(resource.id()));
            unitDetails.put(resource.id(), details);
            units.add(details);
        }

        var content = new VerticalLayout(new H3(getTranslation("report.on-scene")), sceneLastEdit, sceneForm,
                new H3(getTranslation("report.responding-units")), units);
        content.setPadding(false);
        getContent().add(content);

        draft = Signal.computed(() -> {
            var valid = situationError.get() == null && peopleInvolvedError.get() == null
                    && lightlyInjuredError.get() == null && seriouslyInjuredError.get() == null
                    && deceasedError.get() == null
                    && unitValid.values().stream().allMatch(Signal::get);
            if (!valid) {
                return Optional.empty();
            }
            var answers = new HashMap<ResourceId, UnitAnswers>();
            unitAnswers.forEach((resource, signal) -> answers.put(resource, signal.get()));
            return Optional.of(new ReportDraft(new SceneDetails(new ReportText(situation.get()), peopleInvolved.get(),
                    lightlyInjured.get(), seriouslyInjured.get(), deceased.get()), answers));
        });
    }

    @Override
    protected Signal<Optional<ReportDraft>> draft() {
        return draft;
    }

    /**
     * Shows the errors of every field, and returns the draft if there are none. Opens the units that have errors, so
     * that the errors don't hide in a closed unit.
     */
    @Override
    public Optional<ReportDraft> validate() {
        var result = super.validate();
        unitValid.forEach((resource, valid) -> {
            if (!valid.peek()) {
                unitDetails.get(resource).setOpened(true);
            }
        });
        return result;
    }

    /**
     * Returns the error of the rule between the counts, which is worth telling on its own, or {@code null} if the
     * counts follow it.
     */
    @Nullable String casualtiesError() {
        return casualtiesError.peek();
    }

    /**
     * Returns the units that are open now.
     */
    Set<ResourceId> openedUnits() {
        return unitDetails.entrySet().stream().filter(entry -> entry.getValue().isOpened())
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public void focus() {
        situationDevelopment.focus();
    }

    private Signal<@Nullable String> countError(ValueSignal<@Nullable Integer> count,
                                                @Nullable Signal<@Nullable String> otherwise) {
        return Signal.computed(() -> {
            if (!SceneDetails.isCountValid(count.get())) {
                return getTranslation("report.cannot-be-negative");
            }
            return otherwise == null ? null : otherwise.get();
        });
    }

    private static TextArea createTextArea(String label) {
        var textArea = new TextArea(label);
        textArea.setWidthFull();
        textArea.setMinRows(3);
        textArea.setMaxLength(ReportText.MAX_LENGTH);
        // Send the text while the user types, so that closing the tab right after typing still warns about it
        textArea.setValueChangeMode(ValueChangeMode.LAZY);
        return textArea;
    }

    private static IntegerField createCountField(String label) {
        var field = new IntegerField(label);
        field.setWidthFull();
        field.setMin(0);
        field.setStepButtonsVisible(true);
        return field;
    }

    static Span createLastEditSpan() {
        var span = new Span();
        span.getStyle().set("color", "var(--vaadin-text-color-secondary)").set("font-size", "var(--aura-font-size-s)");
        return span;
    }

    static String formatLastEdit(@Nullable UserStamp lastEdit) {
        return lastEdit == null ? I18NProvider.translate("report.not-filled-in-yet")
                : I18NProvider.translate("report.last-edited", lastEdit.displayName(),
                Formatters.formatDateTime(lastEdit.timestamp()));
    }

    private static Component createUnitSummary(ResourceListItem resource, UnitReport unitReport,
                                               Signal<UnitReportStatus> status) {
        var callSign = new Span(resource.callSign());
        callSign.getStyle().set("font-weight", "600");
        // In the summary, so it shows whether the unit is open or not
        var badge = new Badge();
        badge.bindText(status.map(UnitReportStatus::getLabel));
        badge.bindThemeName(BadgeVariant.SUCCESS.getVariantName(), status.map(UnitReportStatus.FILLED_IN::equals));
        badge.bindThemeName(BadgeVariant.WARNING.getVariantName(), status.map(UnitReportStatus.PARTLY_FILLED_IN::equals));
        var lastEdit = createLastEditSpan();
        lastEdit.setText(formatLastEdit(unitReport.lastEdit()));
        var summary = new HorizontalLayout(callSign, badge, lastEdit);
        summary.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        return summary;
    }

    /**
     * How much of a unit's report has been answered, judging by what is in the form right now, saved or not.
     */
    enum UnitReportStatus {
        NOT_FILLED_IN("report.unit.not-filled-in"),
        PARTLY_FILLED_IN("report.unit.partly-filled-in"),
        FILLED_IN("report.unit.filled-in");

        private final String labelKey;

        UnitReportStatus(String labelKey) {
            this.labelKey = labelKey;
        }

        String getLabel() {
            return I18NProvider.translate(labelKey);
        }

        static UnitReportStatus of(@Nullable String situationOnArrival, @Nullable String actionsTaken) {
            var answered = Stream.of(situationOnArrival, actionsTaken)
                    .filter(answer -> answer != null && !answer.isBlank())
                    .count();
            return answered == 2 ? FILLED_IN : answered == 1 ? PARTLY_FILLED_IN : NOT_FILLED_IN;
        }
    }
}
