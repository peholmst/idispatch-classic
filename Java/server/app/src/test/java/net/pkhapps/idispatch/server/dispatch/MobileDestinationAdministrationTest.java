package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator adds the mobile app of a unit as a destination, and pairs a device with it by scanning the QR code
 * of its pairing link. The key in the link is not stored, so the code is shown only when the key is generated: when the
 * destination is saved for the first time, and when the administrator generates a new key to pair another device.
 */
class MobileDestinationAdministrationTest extends UseCaseTest {

    private static final String MOBILE = "Mobile Destination";
    private static final String RESOURCE = "Resource";
    private static final String QR_CODE = "QR code for pairing the mobile app";
    private static final String PAIRING_LINK = "Pairing link";
    private static final String NEW_KEY = "Generate New Key";

    private Administrator administratorAtWork() {
        return new Administrator(user("mobile-admin", "ADMIN"), DestinationManagementView.class, "Resources");
    }

    @Test
    void administratorAddsAMobileDestinationAndSeesTheCodeForPairingItsApp() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();

        admin.startsAdding(MOBILE);
        assertThat(admin.editorTitle()).isEqualTo("Add Mobile Destination");
        assertThat(admin.qrCode(QR_CODE)).as("No key before saving").isEmpty();
        admin.chooses(RESOURCE, resource.callSign());
        admin.saves();

        assertThat(admin.row(resource.callSign())).containsEntry("Type", MOBILE);
        assertThat(admin.editorTitle()).isEqualTo("Edit Mobile Destination");
        var link = admin.valueOf(PAIRING_LINK);
        assertThat(link).contains("/mobile/#pair=");
        assertThat(admin.qrCode(QR_CODE)).contains(link);
        assertThat(given.mobileKeyHashesFor(resource)).as("Only the hash of the key is stored")
                .containsExactly(sha256(keyIn(link)));
    }

    @Test
    void mobileDestinationWithoutAResourceIsNotSaved() {
        var admin = administratorAtWork();

        admin.startsAdding(MOBILE);
        admin.saves();

        assertThat(admin.errorOf(RESOURCE)).isEqualTo("Please select a resource");
        assertThat(admin.isEditing()).isTrue();
    }

    @Test
    void codeIsNotShownAgainOnceTheAdministratorLeavesTheDestination() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();
        addsMobileDestination(admin, resource.callSign());
        admin.cancels();

        admin.opens(resource.callSign());

        assertThat(admin.qrCode(QR_CODE)).isEmpty();
        assertThat(admin.seesField(PAIRING_LINK)).isFalse();
        assertThat(admin.canDo(NEW_KEY)).isTrue();
    }

    @Test
    void administratorGeneratesANewKeyToPairAnotherDevice() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();
        var oldLink = addsMobileDestination(admin, resource.callSign());
        admin.cancels();
        admin.opens(resource.callSign());

        admin.clicks(NEW_KEY);
        assertThat(admin.isAsked("Generate a new key?")).isTrue();
        admin.confirms();

        var newLink = admin.valueOf(PAIRING_LINK);
        assertThat(newLink).isNotEqualTo(oldLink);
        assertThat(admin.qrCode(QR_CODE)).contains(newLink);
        assertThat(given.mobileKeyHashesFor(resource)).as("The old key no longer works")
                .containsExactly(sha256(keyIn(newLink)));
    }

    @Test
    void administratorChangesTheirMindAboutGeneratingANewKey() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();
        var link = addsMobileDestination(admin, resource.callSign());
        admin.cancels();
        admin.opens(resource.callSign());

        admin.clicks(NEW_KEY);
        admin.keepsEditing();

        assertThat(admin.qrCode(QR_CODE)).isEmpty();
        assertThat(given.mobileKeyHashesFor(resource)).containsExactly(sha256(keyIn(link)));
    }

    @Test
    void changingTheDetailsOfAMobileDestinationKeepsItsKey() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();
        var link = addsMobileDestination(admin, resource.callSign());
        admin.cancels();

        admin.opens(resource.callSign());
        admin.fillsIn("Description", unique("Engine "));
        admin.chooses(RESOURCE, other.callSign());
        admin.saves();

        assertThat(admin.row(other.callSign())).containsEntry("Type", MOBILE);
        assertThat(given.mobileKeyHashesFor(resource)).isEmpty();
        assertThat(given.mobileKeyHashesFor(other)).containsExactly(sha256(keyIn(link)));
    }

    @Test
    void deletedMobileDestinationIsNotGivenANewKey() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();
        addsMobileDestination(admin, resource.callSign());
        admin.deletes();

        admin.togglesShowingDeleted();
        admin.opens(resource.callSign());

        assertThat(admin.canChange(RESOURCE)).as("Deleted destinations are read-only").isFalse();
        assertThat(admin.canDo(NEW_KEY)).isFalse();
    }

    @Test
    void administratorChoosesTheUnitFromTheResourcesNotDeleted() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var deleted = given.resource(unique("CS"), ResourceState.AVAILABLE);
        given.deleteResource(deleted);
        var admin = administratorAtWork();

        admin.startsAdding(MOBILE);

        List<String> choices = admin.choicesOf(RESOURCE);
        assertThat(choices).contains(resource.callSign()).doesNotContain(deleted.callSign());
    }

    /**
     * Adds a mobile destination for the resource and returns its pairing link.
     */
    private static String addsMobileDestination(Administrator admin, String callSign) {
        admin.startsAdding(MOBILE);
        admin.chooses(RESOURCE, callSign);
        admin.saves();
        return admin.valueOf(PAIRING_LINK);
    }

    private static String keyIn(String link) {
        return link.substring(link.indexOf("#pair=") + "#pair=".length());
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
