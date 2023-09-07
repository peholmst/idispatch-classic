
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
 *         &lt;element name="SimpleWAPPushResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "simpleWAPPushResult"
})
@XmlRootElement(name = "SimpleWAPPushResponse")
public class SimpleWAPPushResponse {

    @XmlElement(name = "SimpleWAPPushResult")
    protected String simpleWAPPushResult;

    /**
     * Gets the value of the simpleWAPPushResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSimpleWAPPushResult() {
        return simpleWAPPushResult;
    }

    /**
     * Sets the value of the simpleWAPPushResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSimpleWAPPushResult(String value) {
        this.simpleWAPPushResult = value;
    }

}
