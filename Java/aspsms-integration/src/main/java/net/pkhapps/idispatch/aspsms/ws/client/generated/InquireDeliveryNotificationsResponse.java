
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="InquireDeliveryNotificationsResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "inquireDeliveryNotificationsResult"
})
@XmlRootElement(name = "InquireDeliveryNotificationsResponse")
public class InquireDeliveryNotificationsResponse {

    @XmlElement(name = "InquireDeliveryNotificationsResult")
    protected String inquireDeliveryNotificationsResult;

    /**
     * Gets the value of the inquireDeliveryNotificationsResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getInquireDeliveryNotificationsResult() {
        return inquireDeliveryNotificationsResult;
    }

    /**
     * Sets the value of the inquireDeliveryNotificationsResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setInquireDeliveryNotificationsResult(String value) {
        this.inquireDeliveryNotificationsResult = value;
    }

}
