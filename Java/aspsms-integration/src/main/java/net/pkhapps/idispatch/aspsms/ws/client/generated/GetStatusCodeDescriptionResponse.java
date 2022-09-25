
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element
 * name="GetStatusCodeDescriptionResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt; &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getStatusCodeDescriptionResult"
})
@XmlRootElement(name = "GetStatusCodeDescriptionResponse")
public class GetStatusCodeDescriptionResponse {

    @XmlElement(name = "GetStatusCodeDescriptionResult")
    protected String getStatusCodeDescriptionResult;

    /**
     * Gets the value of the getStatusCodeDescriptionResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGetStatusCodeDescriptionResult() {
        return getStatusCodeDescriptionResult;
    }

    /**
     * Sets the value of the getStatusCodeDescriptionResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGetStatusCodeDescriptionResult(String value) {
        this.getStatusCodeDescriptionResult = value;
    }

}
