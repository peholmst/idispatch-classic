
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element
 * name="SendSimpleTextSMSResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt; &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sendSimpleTextSMSResult"
})
@XmlRootElement(name = "SendSimpleTextSMSResponse")
public class SendSimpleTextSMSResponse {

    @XmlElement(name = "SendSimpleTextSMSResult")
    protected String sendSimpleTextSMSResult;

    /**
     * Gets the value of the sendSimpleTextSMSResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSendSimpleTextSMSResult() {
        return sendSimpleTextSMSResult;
    }

    /**
     * Sets the value of the sendSimpleTextSMSResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSendSimpleTextSMSResult(String value) {
        this.sendSimpleTextSMSResult = value;
    }

}
