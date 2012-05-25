
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
 *         &lt;element name="InputViewName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="XmlData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DataId" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "inputViewName",
    "xmlData",
    "dataId"
})
@XmlRootElement(name = "UpdateData")
public class UpdateData {

    protected WebEOCCredentials credentials;
    @XmlElement(name = "BoardName")
    protected String boardName;
    @XmlElement(name = "InputViewName")
    protected String inputViewName;
    @XmlElement(name = "XmlData")
    protected String xmlData;
    @XmlElement(name = "DataId")
    protected int dataId;

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
     * Gets the value of the inputViewName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInputViewName() {
        return inputViewName;
    }

    /**
     * Sets the value of the inputViewName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInputViewName(String value) {
        this.inputViewName = value;
    }

    /**
     * Gets the value of the xmlData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXmlData() {
        return xmlData;
    }

    /**
     * Sets the value of the xmlData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXmlData(String value) {
        this.xmlData = value;
    }

    /**
     * Gets the value of the dataId property.
     * 
     */
    public int getDataId() {
        return dataId;
    }

    /**
     * Sets the value of the dataId property.
     * 
     */
    public void setDataId(int value) {
        this.dataId = value;
    }

}
