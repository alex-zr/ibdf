package hl.ibfd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.Objects;

@Service
public class CollectionLookupService {

    private static final Logger log = LoggerFactory.getLogger(CollectionLookupService.class);

    private final XmlLoaderService xmlLoaderService;

    public CollectionLookupService(XmlLoaderService xmlLoaderService) {
        this.xmlLoaderService = xmlLoaderService;
    }

    public String findCollectionName(java.nio.file.Path collectionsXml, String collectionCode) {
        Objects.requireNonNull(collectionsXml, "collectionsXml must not be null");
        Objects.requireNonNull(collectionCode, "collectionCode must not be null");
        if (collectionCode.isBlank()) {
            throw new IllegalArgumentException("collectionCode must not be blank");
        }

        Document doc = xmlLoaderService.load(collectionsXml);
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        String expression = "/collections/collection[collection_code='" + escapeXPath(collectionCode) + "']/collection_name";
        try {
            NodeList nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            if (nodes.getLength() == 0) {
                throw new IllegalStateException(
                        "Collection code not found in collections.xml: " + collectionCode);
            }
            String name = nodes.item(0).getTextContent();
            log.debug("Resolved collection '{}' -> '{}'", collectionCode, name);
            return name == null ? "" : name.trim();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to lookup collection name for code: " + collectionCode, e);
        }
    }

    public String readCollectionAttribute(Document articleDoc) {
        Objects.requireNonNull(articleDoc, "articleDoc must not be null");
        Element root = articleDoc.getDocumentElement();
        if (root == null) {
            throw new IllegalStateException("Article document has no root element");
        }
        String attr = root.getAttribute("collection");
        if (attr == null || attr.isBlank()) {
            throw new IllegalStateException(
                    "Root element <" + root.getLocalName() + "> has no 'collection' attribute");
        }
        return attr.trim();
    }

    private String escapeXPath(String input) {
        if (input == null) {
            return "";
        }
        if (!input.contains("'")) {
            return input;
        }
        if (!input.contains("\"")) {
            return "\"" + input + "\"";
        }
        StringBuilder out = new StringBuilder("concat(");
        String[] parts = input.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append(",\"'\",");
            }
            out.append("\"").append(parts[i]).append("\"");
        }
        out.append(")");
        return out.toString();
    }
}
