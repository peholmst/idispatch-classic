
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Java class for vouchers complex type.
 * <p>
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * <p>
 * &lt;pre&gt; &amp;lt;complexType name="vouchers"&amp;gt; &amp;lt;complexContent&amp;gt; &amp;lt;restriction
 * base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt; &amp;lt;sequence&amp;gt; &amp;lt;element name="VoucherCode"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Remark"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Credits"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Created"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;element name="Redeemed"
 * type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt; &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt; &amp;lt;/complexContent&amp;gt; &amp;lt;/complexType&amp;gt; &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "vouchers", propOrder = {
    "voucherCode",
    "remark",
    "credits",
    "created",
    "redeemed"
})
public class Vouchers {

    @XmlElement(name = "VoucherCode")
    protected String voucherCode;
    @XmlElement(name = "Remark")
    protected String remark;
    @XmlElement(name = "Credits")
    protected String credits;
    @XmlElement(name = "Created")
    protected String created;
    @XmlElement(name = "Redeemed")
    protected String redeemed;

    /**
     * Gets the value of the voucherCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getVoucherCode() {
        return voucherCode;
    }

    /**
     * Sets the value of the voucherCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setVoucherCode(String value) {
        this.voucherCode = value;
    }

    /**
     * Gets the value of the remark property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Sets the value of the remark property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRemark(String value) {
        this.remark = value;
    }

    /**
     * Gets the value of the credits property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCredits() {
        return credits;
    }

    /**
     * Sets the value of the credits property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCredits(String value) {
        this.credits = value;
    }

    /**
     * Gets the value of the created property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCreated() {
        return created;
    }

    /**
     * Sets the value of the created property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCreated(String value) {
        this.created = value;
    }

    /**
     * Gets the value of the redeemed property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRedeemed() {
        return redeemed;
    }

    /**
     * Sets the value of the redeemed property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRedeemed(String value) {
        this.redeemed = value;
    }

}
