
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for vouchers complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="vouchers"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="VoucherCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Remark" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Credits" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Created" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Redeemed" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
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
     * @return possible object is {@link String }
     */
    public String getVoucherCode() {
        return voucherCode;
    }

    /**
     * Sets the value of the voucherCode property.
     *
     * @param value allowed object is {@link String }
     */
    public void setVoucherCode(String value) {
        this.voucherCode = value;
    }

    /**
     * Gets the value of the remark property.
     *
     * @return possible object is {@link String }
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Sets the value of the remark property.
     *
     * @param value allowed object is {@link String }
     */
    public void setRemark(String value) {
        this.remark = value;
    }

    /**
     * Gets the value of the credits property.
     *
     * @return possible object is {@link String }
     */
    public String getCredits() {
        return credits;
    }

    /**
     * Sets the value of the credits property.
     *
     * @param value allowed object is {@link String }
     */
    public void setCredits(String value) {
        this.credits = value;
    }

    /**
     * Gets the value of the created property.
     *
     * @return possible object is {@link String }
     */
    public String getCreated() {
        return created;
    }

    /**
     * Sets the value of the created property.
     *
     * @param value allowed object is {@link String }
     */
    public void setCreated(String value) {
        this.created = value;
    }

    /**
     * Gets the value of the redeemed property.
     *
     * @return possible object is {@link String }
     */
    public String getRedeemed() {
        return redeemed;
    }

    /**
     * Sets the value of the redeemed property.
     *
     * @param value allowed object is {@link String }
     */
    public void setRedeemed(String value) {
        this.redeemed = value;
    }

}
