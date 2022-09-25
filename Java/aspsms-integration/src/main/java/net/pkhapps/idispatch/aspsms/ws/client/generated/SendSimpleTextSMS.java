
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.*;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element name="UserKey"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Password"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Recipients"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Originator"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="MessageText"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt; &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userKey",
    "password",
    "recipients",
    "originator",
    "messageText"
})
@XmlRootElement(name = "SendSimpleTextSMS")
public class SendSimpleTextSMS {

    @XmlElement(name = "UserKey")
    protected String userKey;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "Recipients")
    protected String recipients;
    @XmlElement(name = "Originator")
    protected String originator;
    @XmlElement(name = "MessageText")
    protected String messageText;

    /**
     * Gets the value of the userKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * Sets the value of the userKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUserKey(String value) {
        this.userKey = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the recipients property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRecipients() {
        return recipients;
    }

    /**
     * Sets the value of the recipients property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRecipients(String value) {
        this.recipients = value;
    }

    /**
     * Gets the value of the originator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getOriginator() {
        return originator;
    }

    /**
     * Sets the value of the originator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setOriginator(String value) {
        this.originator = value;
    }

    /**
     * Gets the value of the messageText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMessageText() {
        return messageText;
    }

    /**
     * Sets the value of the messageText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setMessageText(String value) {
        this.messageText = value;
    }

}
