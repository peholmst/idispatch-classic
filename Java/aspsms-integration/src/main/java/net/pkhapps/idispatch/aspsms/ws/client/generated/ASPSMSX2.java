
package net.pkhapps.idispatch.aspsms.ws.client.generated;

import javax.xml.namespace.QName;
import javax.xml.ws.*;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.3.3 Generated source version: 2.2
 */
@WebServiceClient(name = "ASPSMSX2", targetNamespace = "https://webservice.aspsms.com/aspsmsx2.asmx", wsdlLocation = "https://soap.aspsms.com/aspsmsx2.asmx?WSDL")
public class ASPSMSX2
        extends Service {

    private final static URL ASPSMSX2_WSDL_LOCATION;
    private final static WebServiceException ASPSMSX2_EXCEPTION;
    private final static QName ASPSMSX2_QNAME = new QName("https://webservice.aspsms.com/aspsmsx2.asmx", "ASPSMSX2");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("https://soap.aspsms.com/aspsmsx2.asmx?WSDL");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ASPSMSX2_WSDL_LOCATION = url;
        ASPSMSX2_EXCEPTION = e;
    }

    public ASPSMSX2() {
        super(__getWsdlLocation(), ASPSMSX2_QNAME);
    }

    public ASPSMSX2(WebServiceFeature... features) {
        super(__getWsdlLocation(), ASPSMSX2_QNAME, features);
    }

    public ASPSMSX2(URL wsdlLocation) {
        super(wsdlLocation, ASPSMSX2_QNAME);
    }

    public ASPSMSX2(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ASPSMSX2_QNAME, features);
    }

    public ASPSMSX2(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ASPSMSX2(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *     returns ASPSMSX2Soap
     */
    @WebEndpoint(name = "ASPSMSX2Soap")
    public ASPSMSX2Soap getASPSMSX2Soap() {
        return super.getPort(new QName("https://webservice.aspsms.com/aspsmsx2.asmx", "ASPSMSX2Soap"), ASPSMSX2Soap.class);
    }

    /**
     *
     * @param features
     *     A list of {&#064;link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the &lt;code&gt;features&lt;/code&gt; parameter will have their default values.
     * @return
     *     returns ASPSMSX2Soap
     */
    @WebEndpoint(name = "ASPSMSX2Soap")
    public ASPSMSX2Soap getASPSMSX2Soap(WebServiceFeature... features) {
        return super.getPort(new QName("https://webservice.aspsms.com/aspsmsx2.asmx", "ASPSMSX2Soap"), ASPSMSX2Soap.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ASPSMSX2_EXCEPTION != null) {
            throw ASPSMSX2_EXCEPTION;
        }
        return ASPSMSX2_WSDL_LOCATION;
    }

}
