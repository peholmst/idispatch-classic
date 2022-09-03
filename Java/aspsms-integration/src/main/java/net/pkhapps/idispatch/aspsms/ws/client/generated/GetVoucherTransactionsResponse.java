
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
 *         &lt;element name="GetVoucherTransactionsResult" type="{https://webservice.aspsms.com/aspsmsx2.asmx}ArrayOfVouchers" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "getVoucherTransactionsResult"
})
@XmlRootElement(name = "GetVoucherTransactionsResponse")
public class GetVoucherTransactionsResponse {

    @XmlElement(name = "GetVoucherTransactionsResult")
    protected ArrayOfVouchers getVoucherTransactionsResult;

    /**
     * Gets the value of the getVoucherTransactionsResult property.
     *
     * @return possible object is {@link ArrayOfVouchers }
     */
    public ArrayOfVouchers getGetVoucherTransactionsResult() {
        return getVoucherTransactionsResult;
    }

    /**
     * Sets the value of the getVoucherTransactionsResult property.
     *
     * @param value allowed object is {@link ArrayOfVouchers }
     */
    public void setGetVoucherTransactionsResult(ArrayOfVouchers value) {
        this.getVoucherTransactionsResult = value;
    }

}
