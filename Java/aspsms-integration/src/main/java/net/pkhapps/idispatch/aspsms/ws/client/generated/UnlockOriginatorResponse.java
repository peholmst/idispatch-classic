
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
 *         &lt;element name="UnlockOriginatorResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "unlockOriginatorResult"
})
@XmlRootElement(name = "UnlockOriginatorResponse")
public class UnlockOriginatorResponse {

    @XmlElement(name = "UnlockOriginatorResult")
    protected String unlockOriginatorResult;

    /**
     * Gets the value of the unlockOriginatorResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUnlockOriginatorResult() {
        return unlockOriginatorResult;
    }

    /**
     * Sets the value of the unlockOriginatorResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUnlockOriginatorResult(String value) {
        this.unlockOriginatorResult = value;
    }

}
