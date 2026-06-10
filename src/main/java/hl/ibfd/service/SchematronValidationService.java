package hl.ibfd.service;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class SchematronValidationService {

    private static final Logger log = LoggerFactory.getLogger(SchematronValidationService.class);

    private static final String SCHXSLT_TRANSPILE_RESOURCE = "content/transpile.xsl";

    private final Processor processor;

    public SchematronValidationService() {
        this.processor = new Processor(false);
        log.info("Initialized Saxon HE processor for Schematron: {}", processor.getSaxonProductVersion());
    }

    public ValidationOutcome validate(Path schematronSchema, Path instanceXml) {
        Objects.requireNonNull(schematronSchema, "schematronSchema must not be null");
        Objects.requireNonNull(instanceXml, "instanceXml must not be null");
        XsltExecutable compiled = transpile(schematronSchema);
        return runValidator(compiled, instanceXml);
    }

    private XsltExecutable transpile(Path schematronSchema) {
        try (InputStream transpileStream = loadTranspileStylesheet()) {
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable transpiler = compiler.compile(new StreamSource(transpileStream));
            XsltTransformer transformer = transpiler.load();
            XdmDestination destination = new XdmDestination();
            try (InputStream is = Files.newInputStream(schematronSchema)) {
                XdmNode source = processor.newDocumentBuilder().build(new StreamSource(is));
                transformer.setInitialContextNode(source);
                transformer.setDestination(destination);
                transformer.transform();
                return compiler.compile(destination.getXdmNode().asSource());
            }
        } catch (SaxonApiException | IOException e) {
            throw new IllegalStateException(
                    "Failed to transpile Schematron schema: " + schematronSchema, e);
        }
    }

    private ValidationOutcome runValidator(XsltExecutable validator, Path instanceXml) {
        try (InputStream is = Files.newInputStream(instanceXml)) {
            XsltTransformer transformer = validator.load();
            XdmNode source = processor.newDocumentBuilder().build(new StreamSource(is));
            XdmDestination destination = new XdmDestination();
            transformer.setInitialContextNode(source);
            transformer.setDestination(destination);
            transformer.transform();
            XdmNode result = destination.getXdmNode();
            String svrl = serialize(result);
            boolean valid = !svrl.contains("<sch:failed-assert")
                    && !svrl.contains("<svrl:failed-assert")
                    && !svrl.contains("<failed-assert");
            log.debug("Schematron validation result: valid={}", valid);
            return new ValidationOutcome(valid, svrl);
        } catch (SaxonApiException | IOException e) {
            throw new IllegalStateException(
                    "Failed to apply Schematron validator to " + instanceXml, e);
        }
    }

    private String serialize(XdmNode node) throws SaxonApiException {
        Serializer out = processor.newSerializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");
        StringWriter writer = new StringWriter();
        out.setOutputWriter(writer);
        processor.writeXdmValue(node, out);
        return writer.toString();
    }

    private InputStream loadTranspileStylesheet() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(SCHXSLT_TRANSPILE_RESOURCE);
        if (is == null) {
            throw new IOException("SchXslt2 transpile stylesheet not found on classpath: "
                    + SCHXSLT_TRANSPILE_RESOURCE);
        }
        return is;
    }

    public static class ValidationOutcome {
        private final boolean valid;
        private final String svrl;

        public ValidationOutcome(boolean valid, String svrl) {
            this.valid = valid;
            this.svrl = svrl;
        }

        public boolean isValid() {
            return valid;
        }

        public String getSvrl() {
            return svrl;
        }
    }
}
