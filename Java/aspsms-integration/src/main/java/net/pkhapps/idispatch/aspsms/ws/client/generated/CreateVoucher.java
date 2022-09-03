
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
 *         &lt;element name="UserKey" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="NumberOfVouchers" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="AmountCreditsPerVoucher" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Remarks" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "userKey",
        "password",
        "numberOfVouchers",
        "amountCreditsPerVoucher",
        "remarks"
})
@XmlRootElement(name = "CreateVoucher")
public class CreateVoucher {

    @XmlElement(name = "UserKey")
    protected String userKey;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "NumberOfVouchers")
    protected String numberOfVouchers;
    @XmlElement(name = "AmountCreditsPerVoucher")
    protected String amountCreditsPerVoucher;
    @XmlElement(name = "Remarks")
    protected String remarks;

    /**
     * Gets the value of the userKey property.
     *
     * @return possible object is {@link String }
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * Sets the value of the userKey property.
     *
     * @param value allowed object is {@link String }
     */
    public void setUserKey(String value) {
        this.userKey = value;
    }

    /**
     * Gets the value of the password property.
     *
     * @return possible object is {@link String }
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the numberOfVouchers property.
     *
     * @return possible object is {@link String }
     */
    public String getNumberOfVouchers() {
        return numberOfVouchers;
    }

    /**
     * Sets the value of the numberOfVouchers property.
     *
     * @param value allowed object is {@link String }
     */
    public void setNumberOfVouchers(String value) {
        this.numberOfVouchers = value;
    }

    /**
     * Gets the value of the amountCreditsPerVoucher property.
     *
     * @return possible object is {@link String }
     */
    public String getAmountCreditsPerVoucher() {
        return amountCreditsPerVoucher;
    }

    /**
     * Sets the value of the amountCreditsPerVoucher property.
     *
     * @param value allowed object is {@link String }
     */
    public void setAmountCreditsPerVoucher(String value) {
        this.amountCreditsPerVoucher = value;
    }

    /**
     * Gets the value of the remarks property.
     *
     * @return possible object is {@link String }
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Sets the value of the remarks property.
     *
     * @param value allowed object is {@link String }
     */
    public void setRemarks(String value) {
        this.remarks = value;
    }

}
