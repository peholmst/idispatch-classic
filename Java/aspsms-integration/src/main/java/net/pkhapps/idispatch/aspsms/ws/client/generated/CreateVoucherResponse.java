
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element
 * name="CreateVoucherResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt; &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "createVoucherResult"
})
@XmlRootElement(name = "CreateVoucherResponse")
public class CreateVoucherResponse {

    @XmlElement(name = "CreateVoucherResult")
    protected String createVoucherResult;

    /**
     * Gets the value of the createVoucherResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCreateVoucherResult() {
        return createVoucherResult;
    }

    /**
     * Sets the value of the createVoucherResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCreateVoucherResult(String value) {
        this.createVoucherResult = value;
    }

}
