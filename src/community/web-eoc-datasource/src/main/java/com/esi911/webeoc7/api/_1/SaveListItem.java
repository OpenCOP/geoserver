
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="listId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="listItemId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="listItemName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="listItemColor" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "listId",
    "listItemId",
    "listItemName",
    "listItemColor"
})
@XmlRootElement(name = "SaveListItem")
public class SaveListItem {

    protected WebEOCCredentials credentials;
    protected int listId;
    protected int listItemId;
    protected String listItemName;
    protected String listItemColor;

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
     * Gets the value of the listId property.
     * 
     */
    public int getListId() {
        return listId;
    }

    /**
     * Sets the value of the listId property.
     * 
     */
    public void setListId(int value) {
        this.listId = value;
    }

    /**
     * Gets the value of the listItemId property.
     * 
     */
    public int getListItemId() {
        return listItemId;
    }

    /**
     * Sets the value of the listItemId property.
     * 
     */
    public void setListItemId(int value) {
        this.listItemId = value;
    }

    /**
     * Gets the value of the listItemName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getListItemName() {
        return listItemName;
    }

    /**
     * Sets the value of the listItemName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setListItemName(String value) {
        this.listItemName = value;
    }

    /**
     * Gets the value of the listItemColor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getListItemColor() {
        return listItemColor;
    }

    /**
     * Sets the value of the listItemColor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setListItemColor(String value) {
        this.listItemColor = value;
    }

}
