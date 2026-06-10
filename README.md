# IBFD XML / XSLT / Schematron Demo

This project demonstrates how to process an IBFD journal article XML file with Java, Saxon HE, XSLT 3.0, and Schematron.

## What It Does

- Loads the sample article XML: `src/main/resources/xml/wtj_2018_04_int_1.xml`
- Validates the XML input
- Looks up the journal title in `src/main/resources/config/collections.xml`
- Transforms the article XML into HTML using XSLT
- Preserves the document outline as nested HTML headings
- Runs Schematron validation for the `pubdate` and `year` fields

## Tech Stack

- Java 21
- Spring Boot 4
- Saxon HE 12.9
- SchXslt2 1.1.1

## Project Structure

```text
src/main/java/hl/ibfd
├── IbfdApplication.java
└── service
    ├── ArticleProcessingService.java
    ├── ArticleTransformService.java
    ├── CollectionLookupService.java
    ├── SchematronValidationService.java
    ├── XmlLoaderService.java
    └── XmlValidationService.java

src/main/resources
├── config/collections.xml
├── schematron/pubdate-validation.sch
├── xml/wtj_2018_04_int_1.xml
└── xslt/article-to-html.xsl
```

## How to Run

### 1. Run the test suite

```bash
mvn test
```

### 2. Generate HTML only

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.arguments=--html
```

This creates:

- `output.html`

### 3. Run Schematron validation only

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.arguments=--schematron
```

This creates:

- `schematron-report.xml`

The application logs whether the Schematron validation passed or failed.

### 4. Run both steps

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.arguments=--all
```

### 5. Show CLI help

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.arguments=--help
```

## Manual Schematron Execution

The assignment describes a two-step Schematron flow with Saxon and SchXslt2:

### Step 1: Transpile Schematron to XSLT

```bash
java -cp "libs/Saxon-HE-12.9.jar:libs/xmlresolver-5.3.3.jar:lib/xmlresolver-5.3.3-data.jar"  net.sf.saxon.Transform  -xsl:src/main/resources/content/transpile.xsl -o:check.xsl -s:src/main/resources/schematron/pubdate-validation.sch
```

This command:

- starts Saxon HE
- uses SchXslt2's `transpile.xsl`
- converts the Schematron schema into an XSLT validator
- writes the generated validator to `check.xsl`

### Step 2: Validate the XML with the generated XSLT

```bash
java -cp "libs/Saxon-HE-12.9.jar:libs/xmlresolver-5.3.3.jar:libs/xmlresolver-5.3.3-data.jar" net.sf.saxon.Transform -xsl:check.xsl -s:src/main/resources/xml/wtj_2018_04_int_1.xml -o:report.xml
```

This command:

- applies the generated validator to the source XML
- writes the SVRL validation report to `report.xml`

## Expected Output

### HTML

The generated HTML should include:

- a `<title>` built from the article title and the journal title
- the article outline as nested `h1` to `h6` headings
- inline markup such as `<sub>` and italic `<i>`
- no unwanted footnotes in the title text

### Schematron

The sample XML is expected to fail Schematron validation because:

- `pubdate="2018-09-07"` does not match the textual date `11 September 2018`
- `<year>2019</year>` does not match the year in `pubdate`

## Notes

- The project uses the sample file from the repository by default.
- Output files are written to the project root.
- The XSLT and Schematron code are intentionally kept separate so each step can be tested independently.

## Suggestions For Future Improvement

- Add a short example of the generated HTML structure.
- Add screenshots or an HTML preview link.
- Include a small troubleshooting section for Saxon and SchXslt2 classpath issues.
- Document the expected exit codes for successful and failed Schematron runs.
- Add sample unit tests or integration tests for the XSLT and Schematron outputs.

