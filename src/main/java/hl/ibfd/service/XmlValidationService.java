package hl.ibfd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class XmlValidationService {

    private static final Logger log = LoggerFactory.getLogger(XmlValidationService.class);

    public ValidationResult validateWellFormed(Document document) {
        Objects.requireNonNull(document, "document must not be null");
        log.debug("Document is well-formed (already verified by parser).");
        return new ValidationResult(true, Collections.emptyList());
    }

    public ValidationResult validateAgainstXsd(Document document, Path xsdFile) {
        Objects.requireNonNull(document, "document must not be null");
        if (xsdFile == null || !Files.exists(xsdFile)) {
            log.debug("No XSD provided, skipping XSD validation.");
            return new ValidationResult(true, Collections.emptyList());
        }
        log.debug("Validating against XSD: {}", xsdFile);
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        CollectingErrorHandler errorHandler = new CollectingErrorHandler();
        try (InputStream is = Files.newInputStream(xsdFile)) {
            Schema schema = sf.newSchema(new StreamSource(is));
            Validator validator = schema.newValidator();
            validator.setErrorHandler(errorHandler);
            validator.validate(new javax.xml.transform.dom.DOMSource(document));
        } catch (SAXException | IOException e) {
            errorHandler.errors.add("XSD validation error: " + e.getMessage());
        }
        boolean valid = errorHandler.errors.isEmpty();
        log.debug("XSD validation result: {} ({} errors)", valid, errorHandler.errors.size());
        return new ValidationResult(valid, errorHandler.errors);
    }

    public ValidationResult validateRequiredElements(Document document, List<String> requiredElementXPaths) {
        Objects.requireNonNull(document, "document must not be null");
        if (requiredElementXPaths == null || requiredElementXPaths.isEmpty()) {
            return new ValidationResult(true, Collections.emptyList());
        }
        List<String> errors = new ArrayList<>();
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        for (String xp : requiredElementXPaths) {
            try {
                Double count = (Double) xpath.evaluate("count(" + xp + ")", document, XPathConstants.NUMBER);
                if (count == null || count == 0d) {
                    errors.add("Required element not found: " + xp);
                }
            } catch (Exception e) {
                errors.add("Failed to evaluate XPath '" + xp + "': " + e.getMessage());
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private static class CollectingErrorHandler implements ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(SAXParseException exception) {
            errors.add("Warning: " + exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) {
            errors.add("Error: " + exception.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) {
            errors.add("Fatal: " + exception.getMessage());
        }
    }
}
