package com.safenavi.backend.sync.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * JAXB binding of the common data.go.kr XML envelope:
 * response/header/{resultCode,resultMsg} and response/body/{items/item*, totalCount}.
 * Item children are kept as raw elements so any dataset's columns can be read by name.
 */
@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlResponse {

    public Header header;
    public Body body;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Header {
        public String resultCode;
        public String resultMsg;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Body {
        public Items items;
        public Integer numOfRows;
        public Integer pageNo;
        public Integer totalCount;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Items {
        @XmlElement(name = "item")
        public List<Item> item = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Item {
        @XmlAnyElement
        public List<Element> fields = new ArrayList<>();
    }
}
