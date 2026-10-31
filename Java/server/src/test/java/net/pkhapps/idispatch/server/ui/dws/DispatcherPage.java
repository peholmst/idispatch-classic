package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxLocator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.ListItem;
import net.pkhapps.idispatch.server.ui.AppShell;
import net.pkhapps.idispatch.server.ui.GridRows;

import java.util.List;
import java.util.Map;

/**
 * Where things are on the dispatcher view: the open assignments and all resources in two lists, and the selected
 * assignment in a form with the resources on it.
 */
public class DispatcherPage extends AppShell {

    private static final String NUMBER = "ID";

    /**
     * The texts on the view that the page finds things by, in one of the user interface languages.
     */
    public record Language(Map<String, String> fromEnglish) {

        public static final Language ENGLISH = new Language(Map.of());
        public static final Language SWEDISH = new Language(Map.ofEntries(
                Map.entry("New Assignment", "Nytt uppdrag"), Map.entry("Type", "Typ"),
                Map.entry("Municipality", "Kommun"), Map.entry("Address", "Adress"),
                Map.entry("No assignment selected", "Inget uppdrag valt"), Map.entry("Resource", "Enhet"),
                Map.entry("State", "Status"), Map.entry("Assignment", "Uppdrag"),
                Map.entry("Open assignments", "Öppna uppdrag"), Map.entry("All resources", "Alla enheter"),
                Map.entry("Resources on the assignment", "Enheter på uppdraget"),
                Map.entry("Available resource", "Tillgänglig enhet"),
                Map.entry("Add resource to the assignment", "Lägg till enheten i uppdraget")));
        public static final Language FINNISH = new Language(Map.ofEntries(
                Map.entry("New Assignment", "Uusi tehtävä"), Map.entry("Type", "Tyyppi"),
                Map.entry("Municipality", "Kunta"), Map.entry("Address", "Osoite"),
                Map.entry("No assignment selected", "Tehtävää ei ole valittu"), Map.entry("Resource", "Yksikkö"),
                Map.entry("State", "Tila"), Map.entry("Assignment", "Tehtävä"),
                Map.entry("Open assignments", "Avoimet tehtävät"), Map.entry("All resources", "Kaikki yksiköt"),
                Map.entry("Resources on the assignment", "Tehtävän yksiköt"),
                Map.entry("Available resource", "Vapaa yksikkö"),
                Map.entry("Add resource to the assignment", "Lisää yksikkö tehtävään")));

        String text(String english) {
            return fromEnglish.getOrDefault(english, english);
        }
    }

    private final Language language;

    public DispatcherPage(BrowserlessUIContext window) {
        this(window, Language.ENGLISH);
    }

    public DispatcherPage(BrowserlessUIContext window, Language language) {
        super(window);
        this.language = language;
    }

    public void open() {
        window.navigate(DispatchView.class);
    }

    public void clickNewAssignment() {
        window.findButton().withText(t("New Assignment")).click();
    }

    public boolean canOpenNewAssignment() {
        return window.findButton().withText(t("New Assignment")).component().isEnabled();
    }

    public boolean showsButton(String text) {
        return window.findButton().withText(text).exists();
    }

    // --- The list of open assignments ---

    public List<String> assignmentNumbers() {
        return assignments().column(t(NUMBER));
    }

    public Map<String, String> assignmentRow(String number) {
        return assignments().rowWhere(t(NUMBER), number);
    }

    public List<String> selectedAssignments() {
        return assignments().selected(t(NUMBER));
    }

    public void selectAssignment(String number) {
        assignments().selectRowWhere(t(NUMBER), number);
    }

    // --- The selected assignment ---

    public boolean showsNoAssignmentSelected() {
        return window.findSpan().withText(t("No assignment selected")).exists();
    }

    public boolean showsAssignmentForm() {
        return window.findTextField().withLabel(t("Address")).exists();
    }

    public void chooseType(String type) {
        window.findComboBox(Object.class).withLabel(t("Type")).selectItem(type);
    }

    public List<String> typeChoices() {
        return window.findComboBox(Object.class).withLabel(t("Type")).getSuggestions();
    }

    public void chooseUrgency(String urgency) {
        window.findComboBox(Object.class).withLabel("Urgency").selectItem(urgency);
    }

    public String urgency() {
        return label(window.findComboBox(Object.class).withLabel("Urgency"));
    }

    public void chooseMunicipality(String municipality) {
        window.findComboBox(Object.class).withLabel(t("Municipality")).selectItem(municipality);
    }

    public List<String> municipalityChoices() {
        return window.findComboBox(Object.class).withLabel(t("Municipality")).getSuggestions();
    }

    public void enterAddress(String address) {
        window.findTextField().withLabel(t("Address")).setValue(address);
    }

    public String address() {
        return window.findTextField().withLabel(t("Address")).component().getValue();
    }

    // --- The resources on the selected assignment ---

    public List<String> availableResources() {
        return window.findComboBox(Object.class).withAriaLabel(t("Available resource")).getSuggestions();
    }

    public void addResource(String callSign) {
        window.findComboBox(Object.class).withAriaLabel(t("Available resource")).selectItem(callSign);
        window.findButton().withAriaLabel(t("Add resource to the assignment")).click();
    }

    public List<String> resourcesOnAssignment() {
        return assigned().column(t("Resource"));
    }

    public List<String> statesOnAssignment() {
        return assigned().column(t("State"));
    }

    public void selectResourceOnAssignment(String callSign) {
        assigned().selectRowWhere(t("Resource"), callSign);
    }

    public boolean canRemoveResources() {
        return window.findButton().withAriaLabel("Remove selected resources from the assignment").component().isEnabled();
    }

    public void clickRemoveResources() {
        window.findButton().withAriaLabel("Remove selected resources from the assignment").click();
    }

    public void clickDispatchAll() {
        window.findButton().withText("Dispatch All").click();
    }

    public boolean canDispatchSelected() {
        return window.findButton().withText("Dispatch Selected").component().isEnabled();
    }

    public void clickDispatchSelected() {
        window.findButton().withText("Dispatch Selected").click();
    }

    public void clickDispatchReserved() {
        window.findButton().withText("Dispatch Reserved").click();
    }

    public void clickCloseAssignment() {
        window.findButton().withText("Close Assignment").click();
    }

    public void clickStateOnAssignment(String callSign) {
        assigned().clickCellWhere(t("Resource"), callSign, t("State"));
    }

    /**
     * The items of the list in the notification, such as what is missing from an assignment that cannot be
     * dispatched.
     */
    public List<String> notificationListItems() {
        return window.findNotification().components().stream()
                .flatMap(notification -> window.find(ListItem.class, notification).all().stream())
                .map(ListItem::getText)
                .toList();
    }

    // --- The list of all resources ---

    public String stateOf(String callSign) {
        return resources().rowWhere(t("Resource"), callSign).get(t("State"));
    }

    public String assignmentOf(String callSign) {
        return resources().rowWhere(t("Resource"), callSign).get(t("Assignment"));
    }

    public void clickStateInResourceList(String callSign) {
        resources().clickCellWhere(t("Resource"), callSign, t("State"));
    }

    // --- The dialog for changing the state of a resource ---

    public String stateDialogTitle() {
        return window.findDialog().component().getHeaderTitle();
    }

    public List<String> stateChoices() {
        return window.findButton().inside(window.findDialog()).components().stream()
                .map(com.vaadin.flow.component.button.Button::getText).toList();
    }

    public void chooseState(String state) {
        window.findButton().withText(state).inside(window.findDialog()).click();
    }

    public boolean showsStateDialog() {
        return window.findDialog().components().stream().findAny().isPresent();
    }

    /**
     * Waits for the changes that other users made to reach this window, as server push does in the browser.
     */
    public void receiveUpdates() {
        window.roundTrip();
    }

    private GridRows assignments() {
        return grid("Open assignments");
    }

    private GridRows resources() {
        return grid("All resources");
    }

    private GridRows assigned() {
        return grid("Resources on the assignment");
    }

    private String t(String english) {
        return language.text(english);
    }

    private GridRows grid(String englishName) {
        var name = t(englishName);
        Grid<?> grid = window.find(Grid.class)
                .withCondition(g -> g.getAriaLabel().filter(name::equals).isPresent())
                .single();
        return GridRows.of(window, grid);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String label(ComboBoxLocator<Object> comboBox) {
        ComboBox box = comboBox.component();
        var value = box.getValue();
        return value == null ? "" : box.getItemLabelGenerator().apply(value).toString();
    }
}
