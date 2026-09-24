package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtils {

    static void dumpNode(Node n) {
        dumpNode(n, "");
    }

    /**
     * Dump out a node for debugging. Recursive fcn
     *
     * @param n
     * @param pre
     */
    static void dumpNode(Node n, String pre) {
        String opre = pre;
        pre += " ";
        System.out.print(opre + "<" + n.getNodeName());
        // dump attribute
        NamedNodeMap attr = n.getAttributes();
        if (attr != null) {
            for (int i = 0; i < attr.getLength(); i++) {
                System.out.print(
                        "\n"
                                + pre
                                + "   "
                                + attr.item(i).getNodeName()
                                + "=\""
                                + attr.item(i).getNodeValue()
                                + "\"");
            }
        }
        System.out.println(">");

        // dump value
        String v = pre + n.getNodeValue();
        if (n.getNodeType() == Node.TEXT_NODE) System.out.println(v);

        // dump sub nodes
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node ln = nList.item(i);
            dumpNode(ln, pre + " ");
        }
        System.out.println(opre + "</" + n.getNodeName() + ">");
    }

    static Document getDocument(File file)
            throws ParserConfigurationException, SAXException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            InputSource inputSource = new InputSource(fis);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder theBuilder = dbf.newDocumentBuilder();
            return theBuilder.parse(inputSource);
        }
    }    
}
