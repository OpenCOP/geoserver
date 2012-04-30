
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="credentials" type="{urn:com:esi911:webeoc7:api:1.0}WebEOCCredentials" minOccurs="0"/>
 *         &lt;element name="IncidentName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="IncidentDateTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="Default" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "credentials",
    "incidentName",
    "incidentDateTime",
    "_default"
})
@XmlRootElement(name = "AddIncident")
public class AddIncident {

    protected WebEOCCredentials credentials;
    @XmlElement(name = "IncidentName")
    protected String incidentName;
    @XmlElement(name = "IncidentDateTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar incidentDateTime;
    @XmlElement(name = "Default")
    protected boolean _default;

    /**
     * Gets the value of the credentials property.
     * 
     * @return
     *     possible object is
     *     {@link WebEOCCredentials }
     *     
     */
    public WebEOCCredentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the value of the credentials property.
     * 
     * @param value
     *     allowed object is
     *     {@link WebEOCCredentials }
     *     
     */
    public void setCredentials(WebEOCCredentials value) {
        this.credentials = value;
    }

    /**
     * Gets the value of the incidentName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIncidentName() {
        return incidentName;
    }

    /**
     * Sets the value of the incidentName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIncidentName(String value) {
        this.incidentName = value;
    }

    /**
     * Gets the value of the incidentDateTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getIncidentDateTime() {
        return incidentDateTime;
    }

    /**
     * Sets the value of the incidentDateTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setIncidentDateTime(XMLGregorianCalendar value) {
        this.incidentDateTime = value;
    }

    /**
     * Gets the value of the default property.
     * 
     */
    public boolean isDefault() {
        return _default;
    }

    /**
     * Sets the value of the default property.
     * 
     */
    public void setDefault(boolean value) {
        this._default = value;
    }

}
