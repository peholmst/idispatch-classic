
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element
 * name="CheckCreditsResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt; &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "checkCreditsResult"
})
@XmlRootElement(name = "CheckCreditsResponse")
public class CheckCreditsResponse {

    @XmlElement(name = "CheckCreditsResult")
    protected String checkCreditsResult;

    /**
     * Gets the value of the checkCreditsResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCheckCreditsResult() {
        return checkCreditsResult;
    }

    /**
     * Sets the value of the checkCreditsResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCheckCreditsResult(String value) {
        this.checkCreditsResult = value;
    }

}
