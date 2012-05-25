
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for WebEOCUser complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WebEOCUser">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Username" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="IsAdministrator" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="IsMultipleUserLogin" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="IsAccountDisabled" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ChangePasswordAtNextLogin" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="IsDualCommitUser" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="PrimaryEmail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Color" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WebEOCUser", propOrder = {
    "username",
    "isAdministrator",
    "isMultipleUserLogin",
    "isAccountDisabled",
    "changePasswordAtNextLogin",
    "isDualCommitUser",
    "primaryEmail",
    "color"
})
public class WebEOCUser {

    @XmlElement(name = "Username")
    protected String username;
    @XmlElement(name = "IsAdministrator")
    protected boolean isAdministrator;
    @XmlElement(name = "IsMultipleUserLogin")
    protected boolean isMultipleUserLogin;
    @XmlElement(name = "IsAccountDisabled")
    protected boolean isAccountDisabled;
    @XmlElement(name = "ChangePasswordAtNextLogin")
    protected boolean changePasswordAtNextLogin;
    @XmlElement(name = "IsDualCommitUser")
    protected boolean isDualCommitUser;
    @XmlElement(name = "PrimaryEmail")
    protected String primaryEmail;
    @XmlElement(name = "Color")
    protected String color;

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the isAdministrator property.
     * 
     */
    public boolean isIsAdministrator() {
        return isAdministrator;
    }

    /**
     * Sets the value of the isAdministrator property.
     * 
     */
    public void setIsAdministrator(boolean value) {
        this.isAdministrator = value;
    }

    /**
     * Gets the value of the isMultipleUserLogin property.
     * 
     */
    public boolean isIsMultipleUserLogin() {
        return isMultipleUserLogin;
    }

    /**
     * Sets the value of the isMultipleUserLogin property.
     * 
     */
    public void setIsMultipleUserLogin(boolean value) {
        this.isMultipleUserLogin = value;
    }

    /**
     * Gets the value of the isAccountDisabled property.
     * 
     */
    public boolean isIsAccountDisabled() {
        return isAccountDisabled;
    }

    /**
     * Sets the value of the isAccountDisabled property.
     * 
     */
    public void setIsAccountDisabled(boolean value) {
        this.isAccountDisabled = value;
    }

    /**
     * Gets the value of the changePasswordAtNextLogin property.
     * 
     */
    public boolean isChangePasswordAtNextLogin() {
        return changePasswordAtNextLogin;
    }

    /**
     * Sets the value of the changePasswordAtNextLogin property.
     * 
     */
    public void setChangePasswordAtNextLogin(boolean value) {
        this.changePasswordAtNextLogin = value;
    }

    /**
     * Gets the value of the isDualCommitUser property.
     * 
     */
    public boolean isIsDualCommitUser() {
        return isDualCommitUser;
    }

    /**
     * Sets the value of the isDualCommitUser property.
     * 
     */
    public void setIsDualCommitUser(boolean value) {
        this.isDualCommitUser = value;
    }

    /**
     * Gets the value of the primaryEmail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrimaryEmail() {
        return primaryEmail;
    }

    /**
     * Sets the value of the primaryEmail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrimaryEmail(String value) {
        this.primaryEmail = value;
    }

    /**
     * Gets the value of the color property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the value of the color property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColor(String value) {
        this.color = value;
    }

}
