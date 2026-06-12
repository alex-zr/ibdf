package hl.ibfd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ArticleProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ArticleProcessingService.class);

    private final XmlLoaderService xmlLoaderService;
    private final XmlValidationService xmlValidationService;
    private final CollectionLookupService collectionLookupService;
    private final ArticleTransformService articleTransformService;
    private final SchematronValidationService schematronValidationService;

    @Value("${app.article-xml:classpath:xml/wtj_2018_04_int_1.xml}")
    private String articleXmlResource;

    @Value("${app.collections-xml:classpath:config/collections.xml}")
    private String collectionsXmlResource;

    @Value("${app.xsl:classpath:xslt/article-to-html.xsl}")
    private String xslResource;

    @Value("${app.schematron:classpath:schematron/pubdate-validation.sch}")
    private String schematronResource;

    public ArticleProcessingService(XmlLoaderService xmlLoaderService,
                                    XmlValidationService xmlValidationService,
                                    CollectionLookupService collectionLookupService,
                                    ArticleTransformService articleTransformService,
                                    SchematronValidationService schematronValidationService) {
        this.xmlLoaderService = xmlLoaderService;
        this.xmlValidationService = xmlValidationService;
        this.collectionLookupService = collectionLookupService;
        this.articleTransformService = articleTransformService;
        this.schematronValidationService = schematronValidationService;
    }

    public ProcessingResult processArticle(Path articleXml, Path collectionsXml, Path xsl) {
        log.info("Loading article XML: {}", articleXml);
        Document articleDoc = xmlLoaderService.load(articleXml);

        log.info("Validating required elements");
        XmlValidationService.ValidationResult requiredValidation =
                xmlValidationService.validateRequiredElements(articleDoc, getRequiredElementXPaths());
        if (!requiredValidation.isValid()) {
            throw new IllegalStateException("Missing required elements: " + requiredValidation.getErrors());
        }

        log.info("Looking up collection name from collections.xml: {}", collectionsXml);
        String collectionCode = collectionLookupService.readCollectionAttribute(articleDoc);
        log.info("Collection code: {}", collectionCode);
        String collectionName = collectionLookupService.findCollectionName(collectionsXml, collectionCode);
        log.info("Collection name: {}", collectionName);

        log.info("Transforming to HTML with XSL: {}", xsl);
        String html = articleTransformService.transformToHtml(articleXml, xsl, collectionName);
        log.info("HTML generated, {} bytes", html.length());

        return new ProcessingResult(articleXml, collectionCode, collectionName, html);
    }

    public ProcessingResult processArticleFromResources() {
        Path article = resolve(articleXmlResource);
        Path collections = resolve(collectionsXmlResource);
        Path xsl = resolve(xslResource);
        return processArticle(article, collections, xsl);
    }

    public SchematronValidationService.ValidationOutcome validateSchematron(Path schematron, Path instance) {
        log.info("Running Schematron validation: schema={}, instance={}", schematron, instance);
        return schematronValidationService.validate(schematron, instance);
    }

    public SchematronValidationService.ValidationOutcome validateSchematronFromResources() {
        Path schematron = resolve(schematronResource);
        Path instance = resolve(articleXmlResource);
        return validateSchematron(schematron, instance);
    }

    public List<String> getRequiredElementXPaths() {
        return List.of(
                "/country-chap/chaphead/title",
                "/country-chap/chaphead/year",
                "/country-chap/chaphead/pubdate",
                "/country-chap/chapbody",
                "/country-chap/@collection"
        );
    }

    private Path resolve(String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource path is null");
        }
        if (resource.startsWith("classpath:")) {
            String path = resource.substring("classpath:".length());
            var url = getClass().getClassLoader().getResource(path);
            if (url == null) {
                throw new IllegalStateException("Resource not found: " + resource);
            }
            try {
                return Path.of(url.toURI());
            } catch (Exception e) {
                throw new IllegalStateException("Invalid resource URI: " + resource, e);
            }
        }
        Path p = Path.of(resource);
        if (!Files.exists(p)) {
            throw new IllegalStateException("File not found: " + p);
        }
        return p;
    }

    public static class ProcessingResult {
        private final Path articleXml;
        private final String collectionCode;
        private final String collectionName;
        private final String html;

        public ProcessingResult(Path articleXml, String collectionCode,
                                String collectionName, String html) {
            this.articleXml = articleXml;
            this.collectionCode = collectionCode;
            this.collectionName = collectionName;
            this.html = html;
        }

        public Path getArticleXml() {
            return articleXml;
        }

        public String getCollectionCode() {
            return collectionCode;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getHtml() {
            return html;
        }
    }
}
