package hl.ibfd;

import hl.ibfd.service.ArticleProcessingService;
import hl.ibfd.service.SchematronValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class IbfdApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IbfdApplication.class);

    private final ArticleProcessingService processingService;

    public IbfdApplication(ArticleProcessingService processingService) {
        this.processingService = processingService;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(IbfdApplication.class);
        app.setLogStartupInfo(false);
        ConfigurableApplicationContext ctx = app.run(args);
        SpringApplication.exit(ctx);
    }

    @Override
    public void run(String... args) throws Exception {
        if (contains(args, "--help") || contains(args, "-h")) {
            printHelp();
            return;
        }

        if (contains(args, "--html")) {
            processHtml();
        }

        if (contains(args, "--schematron")) {
            runSchematron();
        }

        if (contains(args, "--all")) {
            processHtml();
            runSchematron();
        }
    }

    private void processHtml() throws Exception {
        ArticleProcessingService.ProcessingResult result = processingService.processArticleFromResources();
        log.info("===========================================");
        log.info("Article: {}", result.getArticleXml());
        log.info("Collection code: {}", result.getCollectionCode());
        log.info("Collection name: {}", result.getCollectionName());
        log.info("HTML size: {} chars", result.getHtml().length());
        log.info("===========================================");

        Path output = Path.of("output.html");
        Files.writeString(output, result.getHtml());
        log.info("Wrote HTML output to {}", output.toAbsolutePath());
    }

    private void runSchematron() {
        SchematronValidationService.ValidationOutcome outcome =
                processingService.validateSchematronFromResources();
        log.info("===========================================");
        log.info("Schematron valid: {}", outcome.isValid());
        log.info("===========================================");
        try {
            Path report = Path.of("schematron-report.xml");
            Files.writeString(report, outcome.getSvrl());
            log.info("Wrote SVRL report to {}", report.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Could not write schematron-report.xml: {}", e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("IBFD XML/XSLT/Schematron demo");
        System.out.println("Usage: java -jar ibfd.jar [options]");
        System.out.println("  --html         Run XSLT transformation and write output.html");
        System.out.println("  --schematron   Run Schematron validation and write schematron-report.xml");
        System.out.println("  --all          Run both");
    }

    private static boolean contains(String[] args, String key) {
        for (String a : args) {
            if (a.equals(key)) {
                return true;
            }
        }
        return false;
    }
}
