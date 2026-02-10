package org.opendcs.ratings;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {

    private XmlUtil() {
        throw new AssertionError("Utility class");
    }

    public static String getChildElementText(Element element, String tagName) {
        Element child = getChildElement(element, tagName);
        if (child == null) {
            return null;
        }
        return getElementText(child);
    }

    public static Element getChildElement(Element element, String tagName) {
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) n;
                if (elem.getTagName().equals(tagName)) {
                    return elem;
                }
            }
        }
        return null;
    }

    public static List<Element> getChildElements(Element element, String tagName) {
        List<Element> elements = new ArrayList<>();
        boolean matchAll = tagName.equals("*");
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) n;
                if (matchAll || elem.getTagName().equals(tagName)) {
                    elements.add(elem);
                }
            }
        }
        return elements;
    }

    public static String getElementText(Element element) {
        StringBuilder sb = new StringBuilder();

        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                sb.append(n.getNodeValue());
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    public static String getAttributeText(Element element, String name) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            String qname = a.getName();
            String lname = a.getLocalName();
            if (qname.equals(name) || lname != null && lname.equals(name)) {
                return a.getValue();
            }
        }
        return "";
    }

    public static String elementToText(Element element) {
        Transformer t;
        try {
            t = TransformerFactory.newInstance().newTransformer();
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(element), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

    }

    public static Element textToElement(String text) throws RatingException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);              // usually what you want
        Document doc;
        try {
            doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
        } catch (Exception e) {
            throw new RatingException(e);
        }
        return doc.getDocumentElement();
    }
}
