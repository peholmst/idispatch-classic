package net.pkhapps.idispatch.server.control;

import jakarta.annotation.PostConstruct;
import net.pkhapps.idispatch.aspsms.ws.client.generated.ASPSMSX2;
import net.pkhapps.idispatch.aspsms.ws.client.generated.ASPSMSX2Soap;
import net.pkhapps.idispatch.server.entity.DispatchNotification;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.SmsDestination;
import net.pkhapps.idispatch.server.events.DispatchNotificationSent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class SmsDispatcher extends AbstractDispatcher {

    @Autowired
    Environment environment;

    private ASPSMSX2Soap aspsmsx2Soap;
    private String userKey;
    private String password;
    private String originator;

    private boolean enabled = true;

    @PostConstruct
    void init() {
        logger.info("Initializing ASPSMS Gateway");
        aspsmsx2Soap = new ASPSMSX2().getASPSMSX2Soap();

        userKey = environment.getProperty("aspsms.userKey");
        password = environment.getProperty("aspsms.password");
        originator = environment.getProperty("aspsms.originator", "iDispatch");

        if (userKey == null || password == null) {
            logger.warn("No ASPSMS credentials found, SmsDispatcher will be disabled");
            enabled = false;
        }
    }

    @Override
    @Async
    public void onApplicationEvent(DispatchNotificationSent dispatchNotificationSent) {
        final DispatchNotification notification = dispatchNotificationSent.getDispatchNotification();
        dispatch(notification.getDestinationsOfType(SmsDestination.class), notification);
    }

    private void dispatch(Collection<SmsDestination> destinations, DispatchNotification notification) {
        if (!enabled) {
            logger.warn("Received dispatch notification, but SmsDispatcher is disabled");
            return;
        }

        final Set<String> phoneNumbers = destinations
                .stream()
                .flatMap(destination -> destination.getPhoneNumbers().stream())
                .collect(Collectors.toSet());

        if (!phoneNumbers.isEmpty()) {
            final String messageText = buildMessageText(notification);
            logger.debug("Sending SMS \"{}\" to recipients {}", messageText, phoneNumbers);
            String resultCode = aspsmsx2Soap.sendSimpleTextSMS(
                    userKey,
                    password,
                    String.join(";", phoneNumbers),
                    originator,
                    messageText
            );
            if (resultCode.equals("StatusCode:1")) {
                destinations.forEach(destination -> createReceipt(destination, notification));
            } else {
                logger.warn("SMS messages were not successfully sent: {}", resultCode);
            }
        } else {
            logger.warn("No phone numbers to send {} to", notification);
        }
    }

    private String buildMessageText(DispatchNotification notification) {
        final StringBuilder sb = new StringBuilder();
        sb.append(notification.getMunicipality().getName());
        sb.append(",");
        sb.append(notification.getAssignmentType().getCode());
        sb.append(notification.getUrgency());
        sb.append(",");
        sb.append(notification.getAddress());
        sb.append(",");
        sb.append(String.join(",", notification.getAssignedResources().stream().map(Resource::getCallSign).collect(Collectors.toSet())));
        sb.append(",");
        sb.append(notification.getDescription());
        return sb.toString();
    }
}
