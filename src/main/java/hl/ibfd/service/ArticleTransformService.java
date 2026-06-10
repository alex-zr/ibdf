package hl.ibfd.service;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ArticleTransformService {

    private static final Logger log = LoggerFactory.getLogger(ArticleTransformService.class);

    private final Processor processor;

    public ArticleTransformService() {
        this.processor = new Processor(false);
        log.info("Initialized Saxon HE processor: {}", processor.getSaxonProductVersion());
    }

    public String transform(Path articleXml, Path xsl, Map<String, String> parameters) {
        Objects.requireNonNull(articleXml, "articleXml must not be null");
        Objects.requireNonNull(xsl, "xsl must not be null");

        XsltExecutable executable = compileStylesheet(xsl);
        XsltTransformer transformer = executable.load();
        applyParameters(transformer, parameters);

        try (InputStream xmlStream = Files.newInputStream(articleXml)) {
            XdmNode source = processor.newDocumentBuilder().build(new StreamSource(xmlStream));
            XdmDestination destination = new XdmDestination();
            transformer.setInitialContextNode(source);
            transformer.setDestination(destination);
            transformer.transform();
            String result = destination.getXdmNode().toString();
            log.debug("Transformation complete: {} bytes", result.length());
            return result;
        } catch (SaxonApiException | IOException e) {
            throw new IllegalStateException(
                    "XSLT transformation failed for " + articleXml, e);
        }
    }

    public String transformToHtml(Path articleXml, Path xsl, String collectionName) {
        Map<String, String> params = new HashMap<>();
        params.put("collectionName", collectionName == null ? "" : collectionName);
        return transform(articleXml, xsl, params);
    }

    private XsltExecutable compileStylesheet(Path xsl) {
        try (InputStream is = Files.newInputStream(xsl)) {
            StreamSource source = new StreamSource(is);
            source.setSystemId(xsl.toUri().toString());
            return processor.newXsltCompiler().compile(source);
        } catch (SaxonApiException | IOException e) {
            throw new IllegalStateException("Failed to compile XSLT: " + xsl, e);
        }
    }

    private void applyParameters(XsltTransformer transformer, Map<String, String> parameters) {
        if (parameters == null) {
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            transformer.setParameter(new net.sf.saxon.s9api.QName(entry.getKey()),
                    new net.sf.saxon.s9api.XdmAtomicValue(entry.getValue()));
        }
    }
}
