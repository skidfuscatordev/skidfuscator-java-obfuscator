package dev.skidfuscator.obfuscator.command;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.XMLConstants;
import org.w3c.dom.*;

/** Only checksums reviewed in the versioned Gradle verification manifest are trusted automatically. */
public final class TrustedLibraryCatalog {
    private TrustedLibraryCatalog() {}
    public static String checksum(String group,String name,String version) throws Exception {
        try (InputStream input=TrustedLibraryCatalog.class.getResourceAsStream("/security/library-checksums.xml")) {
            if (input==null) throw new IOException("Verified library catalog is missing");
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            Document document=factory.newDocumentBuilder().parse(input);
            NodeList components=document.getElementsByTagNameNS("*","component");
            for (int i=0;i<components.getLength();i++) {
                Element component=(Element)components.item(i);
                if (!group.equals(component.getAttribute("group")) || !name.equals(component.getAttribute("name")) || !version.equals(component.getAttribute("version"))) continue;
                NodeList artifacts=component.getElementsByTagNameNS("*","artifact");
                for (int j=0;j<artifacts.getLength();j++) {
                    Element artifact=(Element)artifacts.item(j);
                    if (!(name+"-"+version+".jar").equals(artifact.getAttribute("name"))) continue;
                    NodeList hashes=artifact.getElementsByTagNameNS("*","sha256");
                    if (hashes.getLength()>0) {
                        String hash=((Element)hashes.item(0)).getAttribute("value");
                        if (hash.matches("[0-9a-f]{64}")) return hash;
                    }
                }
            }
            throw new IOException("This library/version is not in the verified catalog. Verify it externally and use Add Library or --libs.");
        }
    }
}
