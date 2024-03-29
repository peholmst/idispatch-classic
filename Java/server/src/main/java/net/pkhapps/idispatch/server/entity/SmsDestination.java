package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sms_destinations")
public class SmsDestination extends Destination {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sms_destination_phone_numbers")
    @NotEmpty(message = "Please enter at least one phone number")
    private Set<String> phoneNumbers = new HashSet<>();

    public Set<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(Set<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        final SmsDestination clone = (SmsDestination) super.clone();
        clone.phoneNumbers = new HashSet<>(phoneNumbers);
        return clone;
    }

    @Override
    public String getType() {
        return "SMS Destination";
    }
}
