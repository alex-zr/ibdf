package hl.ibfd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class XmlLoaderService {

    private static final Logger log = LoggerFactory.getLogger(XmlLoaderService.class);

    public boolean exists(Path file) {
        return file != null && Files.exists(file) && Files.isRegularFile(file);
    }

    public Document load(Path file) {
        if (!exists(file)) {
            throw new IllegalArgumentException("XML file not found: " + file);
        }
        log.debug("Loading XML file: {}", file);
        DocumentBuilderFactory dbf = createSecureFactory();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            try (InputStream is = Files.newInputStream(file)) {
                return builder.parse(is);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Failed to parse XML file: " + file, e);
        }
    }

    private DocumentBuilderFactory createSecureFactory() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception e) {
            log.warn("Could not set secure features on DocumentBuilderFactory: {}", e.getMessage());
        }
        dbf.setNamespaceAware(true);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }
}
