package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.flow.data.provider.SortDirection;

import java.util.List;
import java.util.Map;

/**
 * Actor: a dispatcher at the dispatcher workstation, in their own browser window. Knows what a dispatcher does; leaves
 * where things are on the screen to {@link DispatcherPage}.
 */
public class Dispatcher {

    private final DispatcherPage page;

    /**
     * Logs in as the given user and opens the dispatcher view.
     */
    public Dispatcher(BrowserlessUserContext user) {
        this(user, DispatcherPage.Language.ENGLISH);
    }

    /**
     * Logs in as the given user, whose user interface is in the given language, and opens the dispatcher view.
     */
    public Dispatcher(BrowserlessUserContext user, DispatcherPage.Language language) {
        page = new DispatcherPage(user.newWindow(), language);
        page.open();
    }

    /**
     * The title of the view and the texts of the buttons in its toolbar, as the dispatcher sees them.
     */
    public String viewTitle() {
        return page.viewTitle().orElse("");
    }

    public boolean seesButton(String text) {
        return page.showsButton(text);
    }

    /**
     * Opens a new assignment and returns its number.
     */
    public String opensANewAssignment() {
        var before = page.assignmentNumbers();
        page.clickNewAssignment();
        return page.assignmentNumbers().stream()
                .filter(number -> !before.contains(number))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No new assignment in the list"));
    }

    public void choosesType(String type) {
        page.chooseType(type);
    }

    public void choosesUrgency(String urgency) {
        page.chooseUrgency(urgency);
    }

    public void choosesMunicipality(String municipality) {
        page.chooseMunicipality(municipality);
    }

    public void entersAddress(String address) {
        page.enterAddress(address);
    }

    /**
     * Fills in everything an assignment needs before it can be dispatched.
     */
    public void fillsIn(String type, String municipality, String address) {
        choosesType(type);
        choosesMunicipality(municipality);
        entersAddress(address);
    }

    public void selectsAssignment(String number) {
        page.selectAssignment(number);
    }

    public void addsResource(String callSign) {
        page.addResource(callSign);
    }

    public void removesResource(String callSign) {
        page.clickRemoveResource(callSign);
    }

    public void dispatchesAll() {
        page.clickDispatchAll();
    }

    public void dispatches(String callSign) {
        page.selectResourceOnAssignment(callSign);
        page.clickDispatchSelected();
    }

    public void dispatchesReserved() {
        page.clickDispatchReserved();
    }

    public void closesTheAssignment() {
        page.clickCloseAssignment();
    }

    /**
     * Changes the state of a resource on the selected assignment, as when the unit reports over the radio.
     */
    public void setsStateOnAssignment(String callSign, String state) {
        page.clickStateOnAssignment(callSign);
        page.chooseState(state);
    }

    /**
     * Opens the state choices of a resource in the list of all resources.
     */
    public void opensStateOf(String callSign) {
        page.clickStateInResourceList(callSign);
    }

    public void choosesState(String state) {
        page.chooseState(state);
    }

    public void receivesUpdates() {
        page.receiveUpdates();
    }

    // --- What the dispatcher sees ---

    public List<String> openAssignments() {
        return page.assignmentNumbers();
    }

    public boolean seesAssignment(String number) {
        return page.assignmentNumbers().contains(number);
    }

    /**
     * The assignment as the list shows it, column header to text.
     */
    public Map<String, String> assignmentInList(String number) {
        return page.assignmentRow(number);
    }

    public boolean hasSelected(String number) {
        return page.selectedAssignments().equals(List.of(number));
    }

    public boolean seesAnAssignmentToEdit() {
        return page.showsAssignmentForm() && !page.showsNoAssignmentSelected();
    }

    public boolean isToldNoAssignmentIsSelected() {
        return page.showsNoAssignmentSelected() && !page.showsAssignmentForm();
    }

    public boolean canOpenANewAssignment() {
        return page.canOpenNewAssignment();
    }

    public String urgency() {
        return page.urgency();
    }

    public void writesDescription(String description) {
        page.enterDescription(description);
    }

    public String description() {
        return page.description();
    }

    public String address() {
        return page.address();
    }

    public String type() {
        return page.type();
    }

    public String municipality() {
        return page.municipality();
    }

    public List<String> typeChoices() {
        return page.typeChoices();
    }

    public List<String> municipalityChoices() {
        return page.municipalityChoices();
    }

    public List<String> availableResources() {
        return page.availableResources();
    }

    public List<String> resourcesOnAssignment() {
        return page.resourcesOnAssignment();
    }

    public List<String> statesOnAssignment() {
        return page.statesOnAssignment();
    }

    /**
     * Whether the box for selecting every resource on the assignment is ticked.
     */
    public boolean seesAllResourcesOnAssignmentSelected() {
        return page.allResourcesOnAssignmentTicked();
    }

    public boolean canDispatchSelected() {
        return page.canDispatchSelected();
    }

    public void filtersResourcesBy(String text) {
        page.filterResources(text);
    }

    public void sortsResourcesBy(String column, SortDirection direction) {
        page.sortResourcesBy(column, direction);
    }

    /**
     * Whether the first resource is above the second in the list of all resources.
     */
    public boolean seesResourceAbove(String upper, String lower) {
        return page.indexInResourceList(upper) < page.indexInResourceList(lower);
    }

    public List<String> listedResources() {
        return page.listedResources();
    }

    public String stateOf(String callSign) {
        return page.stateOf(callSign);
    }

    public String assignmentOf(String callSign) {
        return page.assignmentOf(callSign);
    }

    public List<String> missingInformation() {
        return page.notificationListItems();
    }

    public boolean wasToldAnything() {
        return !page.notifications().isEmpty();
    }

    public String stateQuestion() {
        return page.stateDialogTitle();
    }

    public List<String> stateChoices() {
        return page.stateChoices();
    }

    public boolean isChoosingAState() {
        return page.showsStateDialog();
    }
}
