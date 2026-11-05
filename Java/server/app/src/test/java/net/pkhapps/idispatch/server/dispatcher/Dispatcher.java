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
    private final ResourceStatusPage statusPage;
    private final MobilePhonesPage phonesPage;

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
        var window = user.newWindow();
        page = new DispatcherPage(window, language);
        statusPage = new ResourceStatusPage(window);
        phonesPage = new MobilePhonesPage(window);
        page.open();
    }

    /**
     * The title of the view and the texts of the buttons in its toolbar, as the dispatcher sees them.
     */
    public String viewTitle() {
        return page.viewTitle().orElse("");
    }

    /**
     * Who last changed the open assignment and when, such as "Last edited by Dora Dispatcher on …".
     */
    public String seesWhoLastChangedIt() {
        return page.lastChange();
    }

    public void opensTheHistory() {
        page.openHistory();
    }

    /**
     * The events in the open history, newest first, by the Time, User and Event columns.
     */
    public java.util.List<java.util.Map<String, String>> history() {
        return page.history();
    }

    public void closesTheHistory() {
        page.closeHistory();
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

    public boolean canCloseTheAssignment() {
        return page.canCloseAssignment();
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

    /**
     * Opens the view with a card for every resource.
     */
    public void opensResourceStatus() {
        statusPage.open();
    }

    public void filtersResourceStatusByCallSign(String text) {
        statusPage.filterByCallSign(text);
    }

    public void filtersResourceStatusByStates(String... states) {
        statusPage.filterByStates(states);
    }

    public void clearsTheStateFilter() {
        statusPage.clearStateFilter();
    }

    /**
     * Opens the view with every unit's mobile phone and the unit it is on.
     */
    public void opensMobilePhones() {
        phonesPage.open();
    }

    public void filtersPhonesBy(String text) {
        phonesPage.filterBy(text);
    }

    /**
     * Starts moving the phone to another unit, by clicking the unit it is on now.
     */
    public void startsMovingPhone(String phone) {
        phonesPage.clickUnitOf(phone);
    }

    public void choosesNewUnit(String callSign) {
        phonesPage.chooseUnit(callSign);
    }

    public void savesTheNewUnit() {
        phonesPage.save();
    }

    public void cancelsMovingThePhone() {
        phonesPage.cancel();
    }

    /**
     * Moves the phone to the unit, from the mobile phones view.
     */
    public void movesPhone(String phone, String callSign) {
        startsMovingPhone(phone);
        choosesNewUnit(callSign);
        savesTheNewUnit();
    }

    public void receivesPhoneUpdates() {
        phonesPage.receiveUpdates();
    }

    /**
     * The phones listed on the mobile phones view, from top to bottom.
     */
    public List<String> listedPhones() {
        return phonesPage.phones();
    }

    public String unitOfPhone(String phone) {
        return phonesPage.unitOf(phone);
    }

    public String phonesEmptyText() {
        return phonesPage.emptyText();
    }

    public String movingQuestion() {
        return phonesPage.dialogTitle();
    }

    public List<String> newUnitChoices() {
        return phonesPage.unitChoices();
    }

    public boolean isMovingAPhone() {
        return phonesPage.showsDialog();
    }

    // --- What the dispatcher sees ---

    /**
     * The call signs on the resource status cards, in the order they are shown.
     */
    public List<String> resourceCards() {
        return statusPage.callSigns();
    }

    public String stateOnCardOf(String callSign) {
        return statusPage.stateOf(callSign);
    }

    public String assignmentOnCardOf(String callSign) {
        return statusPage.assignmentOf(callSign);
    }

    public boolean isToldNoResourcesMatch() {
        return statusPage.showsNoResources();
    }

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

    public boolean canDispatchAll() {
        return page.canDispatchAll();
    }

    public boolean canDispatchReserved() {
        return page.canDispatchReserved();
    }

    public void filtersAvailableResourcesBy(String text) {
        page.filterAvailableResources(text);
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
