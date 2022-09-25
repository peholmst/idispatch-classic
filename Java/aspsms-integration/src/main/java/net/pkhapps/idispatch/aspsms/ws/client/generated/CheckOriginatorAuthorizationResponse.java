
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element
 * name="CheckOriginatorAuthorizationResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt; &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "checkOriginatorAuthorizationResult"
})
@XmlRootElement(name = "CheckOriginatorAuthorizationResponse")
public class CheckOriginatorAuthorizationResponse {

    @XmlElement(name = "CheckOriginatorAuthorizationResult")
    protected String checkOriginatorAuthorizationResult;

    /**
     * Gets the value of the checkOriginatorAuthorizationResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCheckOriginatorAuthorizationResult() {
        return checkOriginatorAuthorizationResult;
    }

    /**
     * Sets the value of the checkOriginatorAuthorizationResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCheckOriginatorAuthorizationResult(String value) {
        this.checkOriginatorAuthorizationResult = value;
    }

}
