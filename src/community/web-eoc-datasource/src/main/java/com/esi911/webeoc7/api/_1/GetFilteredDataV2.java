
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="BoardName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DisplayViewName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ViewFilterNames" type="{urn:com:esi911:webeoc7:api:1.0}ArrayOfString" minOccurs="0"/>
 *         &lt;element name="XmlUserFilter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CustomFilter" type="{urn:com:esi911:webeoc7:api:1.0}CustomFilter" minOccurs="0"/>
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
    "boardName",
    "displayViewName",
    "viewFilterNames",
    "xmlUserFilter",
    "customFilter"
})
@XmlRootElement(name = "GetFilteredDataV2")
public class GetFilteredDataV2 {

    protected WebEOCCredentials credentials;
    @XmlElement(name = "BoardName")
    protected String boardName;
    @XmlElement(name = "DisplayViewName")
    protected String displayViewName;
    @XmlElement(name = "ViewFilterNames")
    protected ArrayOfString viewFilterNames;
    @XmlElement(name = "XmlUserFilter")
    protected String xmlUserFilter;
    @XmlElement(name = "CustomFilter")
    protected CustomFilter customFilter;

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
     * Gets the value of the boardName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBoardName() {
        return boardName;
    }

    /**
     * Sets the value of the boardName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBoardName(String value) {
        this.boardName = value;
    }

    /**
     * Gets the value of the displayViewName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisplayViewName() {
        return displayViewName;
    }

    /**
     * Sets the value of the displayViewName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisplayViewName(String value) {
        this.displayViewName = value;
    }

    /**
     * Gets the value of the viewFilterNames property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfString }
     *     
     */
    public ArrayOfString getViewFilterNames() {
        return viewFilterNames;
    }

    /**
     * Sets the value of the viewFilterNames property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfString }
     *     
     */
    public void setViewFilterNames(ArrayOfString value) {
        this.viewFilterNames = value;
    }

    /**
     * Gets the value of the xmlUserFilter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXmlUserFilter() {
        return xmlUserFilter;
    }

    /**
     * Sets the value of the xmlUserFilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXmlUserFilter(String value) {
        this.xmlUserFilter = value;
    }

    /**
     * Gets the value of the customFilter property.
     * 
     * @return
     *     possible object is
     *     {@link CustomFilter }
     *     
     */
    public CustomFilter getCustomFilter() {
        return customFilter;
    }

    /**
     * Sets the value of the customFilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link CustomFilter }
     *     
     */
    public void setCustomFilter(CustomFilter value) {
        this.customFilter = value;
    }

}
