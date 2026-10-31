package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsLocator;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import net.pkhapps.idispatch.server.ui.AppShell;
import net.pkhapps.idispatch.server.ui.GridRows;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Where things are on the reports view: the closed assignments in a list with filters above it, and the report of the
 * selected assignment beside the list, or on top of it on a narrow screen.
 */
public class ReportsPage extends AppShell {

    static final String NUMBER = "Number";
    static final String REPORT = "Report";

    public ReportsPage(BrowserlessUIContext window) {
        super(window);
    }

    public void open() {
        window.navigate(ReportsView.class);
    }

    // --- The filters ---

    public void filterClosedFrom(LocalDate date) {
        window.findDatePicker().withLabel("Closed from").setValue(date);
    }

    public void filterClosedTo(LocalDate date) {
        window.findDatePicker().withLabel("Closed to").setValue(date);
    }

    public void clearClosedFrom() {
        window.findDatePicker().withLabel("Closed from").clear();
    }

    public void clearClosedTo() {
        window.findDatePicker().withLabel("Closed to").clear();
    }

    public LocalDate closedFrom() {
        return window.findDatePicker().withLabel("Closed from").component().getValue();
    }

    /**
     * Chooses an item in one of the filters: Municipality, Type or Report.
     */
    public void filter(String filter, String item) {
        window.findComboBox(Object.class).withLabel(filter).selectItem(item);
    }

    public void clearFilter(String filter) {
        window.findComboBox(Object.class).withLabel(filter).clickClearButton();
    }

    public List<String> filterChoices(String filter) {
        return window.findComboBox(Object.class).withLabel(filter).getSuggestions();
    }

    /**
     * The item chosen in the filter, or an empty string if none is.
     */
    public String filterValue(String filter) {
        var comboBox = window.findComboBox(Object.class).withLabel(filter).component();
        var value = comboBox.getValue();
        return value == null ? "" : comboBox.getItemLabelGenerator().apply(value);
    }

    public void clickClearFilters() {
        window.findButton().withText("Clear").click();
    }

    public void clickRefresh() {
        window.findButton().withText("Refresh").click();
    }

    // --- The list of closed assignments ---

    public List<String> listedNumbers() {
        return list().column(NUMBER);
    }

    public Map<String, String> rowOf(String number) {
        return list().rowWhere(NUMBER, number);
    }

    public void sortBy(String column, SortDirection direction) {
        list().sortBy(column, direction);
    }

    public String emptyListText() {
        return list().size() == 0 ? list().emptyStateText() : "";
    }

    public void select(String number) {
        list().selectRowWhere(NUMBER, number);
    }

    public List<String> selectedNumbers() {
        return list().selected(NUMBER);
    }

    /**
     * Whether the report status in the list is shown as done, in green.
     */
    @SuppressWarnings("unchecked")
    public boolean listShowsReportAsDone(String number) {
        Grid<Object> grid = gridWithColumn(REPORT);
        var row = list().indexOfRowWhere(NUMBER, number);
        var badge = (Badge) new com.vaadin.flow.component.grid.GridTester<>(grid)
                .getCellComponent(row, grid.getColumns().indexOf(grid.getColumns().stream()
                        .filter(c -> REPORT.equals(c.getHeaderText())).findFirst().orElseThrow()));
        return badge.getThemeNames().contains("success");
    }

    private GridRows list() {
        return GridRows.of(window, gridWithColumn(REPORT));
    }

    // --- The report ---

    public boolean showsReport() {
        return detail() != null;
    }

    public boolean showsNoAssignmentSelected() {
        return window.findSpan().withText("No assignment selected").exists();
    }

    /**
     * Closes the report that is shown on top of the list by pressing Esc. The master-detail layout has no locator, so
     * this fires the event that the layout's web component sends to the server.
     */
    public void pressEscapeOnReport() {
        var layout = view();
        ComponentUtil.fireEvent(layout, new MasterDetailLayout.DetailEscapePressEvent(layout, true));
    }

    /**
     * Closes the report that is shown on top of the list by clicking beside it. The master-detail layout has no
     * locator, so this fires the event that the layout's web component sends to the server.
     */
    public void clickBesideReport() {
        var layout = view();
        ComponentUtil.fireEvent(layout, new MasterDetailLayout.BackdropClickEvent(layout, true));
    }

    /**
     * The value of one of the read-only fields at the top of the report, such as Number or Address.
     */
    public String field(String label) {
        return window.find(TextField.class, formItem(label)).single().getValue();
    }

    public boolean canChangeField(String label) {
        return !window.find(TextField.class, formItem(label)).single().isReadOnly();
    }

    public String description() {
        return window.find(com.vaadin.flow.component.textfield.TextArea.class, formItem("Description")).single()
                .getValue();
    }

    private FormLayout.FormItem formItem(String label) {
        return window.find(FormLayout.FormItem.class)
                .withCondition(item -> label.equals(item.getElement().getTextRecursively()))
                .single();
    }

    /**
     * The resources that responded, as rows of the timeline: the call sign and the time of each state.
     */
    public List<Map<String, String>> timeline() {
        return GridRows.of(window, gridWithColumn("Dispatched")).all();
    }

    public String status() {
        return statusBadge().getText();
    }

    public boolean showsStatusAsDone() {
        return statusBadge().getThemeNames().contains("success");
    }

    private Badge statusBadge() {
        return window.find(Badge.class)
                .withCondition(badge -> List.of("Open", "Closed").contains(badge.getText()))
                .withCondition(badge -> isInside(badge, detail()))
                .single();
    }

    public boolean showsTextStartingWith(String start) {
        return window.findSpan().withCondition(span -> span.getText() != null && span.getText().startsWith(start))
                .exists();
    }

    /**
     * Who last edited what happened on scene, or that nobody has yet.
     */
    public String sceneLastEdit() {
        return window.findSpan()
                .withCondition(span -> isLastEditText(span.getText()))
                .withCondition(span -> !isInsideAnyUnit(span))
                .component().getText();
    }

    public void enterSituationDevelopment(String text) {
        window.findTextArea().withLabel("How did the situation develop?").setValue(text);
    }

    public String situationDevelopment() {
        return window.findTextArea().withLabel("How did the situation develop?").component().getValue();
    }

    public boolean canChangeSituationDevelopment() {
        return !window.findTextArea().withLabel("How did the situation develop?").component().isReadOnly();
    }

    /**
     * Enters a number in one of the count fields, such as "How many people were involved?".
     */
    public void enterCount(String label, Integer count) {
        window.findIntegerField().withLabel(label).setValue(count);
    }

    public Integer count(String label) {
        return window.findIntegerField().withLabel(label).component().getValue();
    }

    public boolean canChangeCount(String label) {
        return !window.findIntegerField().withLabel(label).component().isReadOnly();
    }

    // --- The units that responded ---

    public void openUnit(String callSign) {
        unit(callSign).openDetails();
    }

    public boolean isUnitOpen(String callSign) {
        return unit(callSign).isOpen();
    }

    public void enterForUnit(String callSign, String question, String answer) {
        window.findTextArea().withLabel(question).inside(unit(callSign)).setValue(answer);
    }

    public String answerOfUnit(String callSign, String question) {
        return window.findTextArea().withLabel(question).inside(unit(callSign)).component().getValue();
    }

    public boolean canAnswerForUnit(String callSign, String question) {
        return !window.findTextArea().withLabel(question).inside(unit(callSign)).component().isReadOnly();
    }

    /**
     * The badge next to the unit's call sign, telling how much of its report is filled in.
     */
    public String unitBadge(String callSign) {
        return unitBadgeComponent(callSign).getText();
    }

    /**
     * The color of the unit's badge: "success" (green), "warning" (yellow), or "" for the default.
     */
    public String unitBadgeColor(String callSign) {
        var themes = unitBadgeComponent(callSign).getThemeNames();
        return themes.contains("success") ? "success" : themes.contains("warning") ? "warning" : "";
    }

    private Badge unitBadgeComponent(String callSign) {
        return window.find(Badge.class, unit(callSign).component().getSummary()).single();
    }

    /**
     * Who last edited the unit's report, or that nobody has yet, as shown next to the call sign.
     */
    public String unitLastEdit(String callSign) {
        return window.findSpan().inside(unit(callSign).component().getSummary())
                .withCondition(span -> isLastEditText(span.getText()))
                .component().getText();
    }

    private DetailsLocator unit(String callSign) {
        return window.findDetails().withCondition(details -> details.getSummary() != null
                && details.getSummary().getElement().getTextRecursively().startsWith(callSign));
    }

    private boolean isInsideAnyUnit(Component component) {
        return window.find(Details.class).all().stream().anyMatch(details -> isInside(component, details));
    }

    // --- The actions ---

    public boolean showsButton(String text) {
        return window.findButton().withText(text).exists();
    }

    public void click(String button) {
        window.findButton().withText(button).click();
    }

    private MasterDetailLayout view() {
        return window.findMasterDetailLayout().component();
    }

    private Component detail() {
        return view().getDetail();
    }

    private static boolean isLastEditText(String text) {
        return text != null && (text.equals("Not filled in yet") || text.startsWith("Last edited by "));
    }

    private static boolean isInside(Component component, Component container) {
        if (container == null) {
            return false;
        }
        var parent = component.getParent();
        while (parent.isPresent()) {
            if (parent.get() == container) {
                return true;
            }
            parent = parent.get().getParent();
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Grid<Object> gridWithColumn(String header) {
        return (Grid) window.find(Grid.class)
                .withCondition(grid -> ((Grid<?>) grid).getColumns().stream()
                        .anyMatch(column -> header.equals(column.getHeaderText())))
                .single();
    }
}
