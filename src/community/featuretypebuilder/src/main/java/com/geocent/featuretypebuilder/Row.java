package com.geocent.featuretypebuilder;

import java.io.Serializable;

/**
 *
 * @author thanthese
 */
public class Row implements Serializable {
  private String name;
  private String type;

  /**
   * Does some work towards turning name into something valid.
   *
   * @param name
   * @param type
   */
  public Row(String name, String type) {
    this.name = correctName(name);
    this.type = type;
  }
  public String getName() { return name; }
  public String getType() { return type; }

  /**
   * Two functions:
   *
   * 1.  "hello world" -> "hello_world"
   * 2.  "1test"       -> "_1test"
   *
   * @param name
   * @return
   */
  public static String correctName(String name) {
    String n = name.toLowerCase().replace(" ", "_");
    boolean startsWithNum = n.length() > 0 && Character.isDigit(n.charAt(0));
    return startsWithNum ? "_" + n : n;
  }

  @Override
  public String toString() {
    return "Row{" + "name=" + name + ", type=" + type + '}';
  }
}