package org.opendcs.ratings;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XmlUtil {

    private XmlUtil() {
        throw new AssertionError("Utility class");
    }

    public static String elementText(Element element) {
        StringBuilder sb = new StringBuilder();

        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                sb.append(n.getNodeValue());
            }
        }

        return sb.toString().trim();
    }

    public static String attributeText(Element element, String name) {
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
}
