# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=FileConvertTest

# Package into JAR
mvn package

# Run the application
java -jar target/file-processor-1.0.0.jar

# Run with Maven Spring Boot plugin
mvn spring-boot:run
```

## Architecture Overview

This is a Spring Boot 3 file processing REST API service using Java 21.

### Layer Structure

- **Controller** (`FileConvertController`): REST endpoints, input validation, delegates to service
- **Service** (`FileConvertService`): Business logic, file validation, orchestrates utility classes
- **Util**: Static utility classes containing the actual file processing logic
  - `DocConverter`: DOC to DOCX conversion using Apache POI HWPF/XWPF
  - `ZipProcessor`: ZIP extraction and HTML-to-text conversion using Jsoup
  - `TextExtractor`: Extract text from DOC/DOCX/PDF/PPT/PPTX using POI and PDFBox

### Key Design Patterns

- **Builder pattern**: `FileResponse` uses a builder for construction
- **Static utility classes**: File processing logic is in stateless static methods
- **Validation**: Jakarta Validation annotations on `FileConvertRequest` with `@Valid` in controllers
- **Global exception handling**: `GlobalExceptionHandler` uses `@RestControllerAdvice` to map exceptions to `FileResponse`

### Dependencies

- **Apache POI** (5.2.5): Office document processing - uses `HWPFDocument` for old .doc format, `XWPFDocument` for .docx, `HSLFSlideShow` for .ppt, `XMLSlideShow` for .pptx
- **Apache PDFBox** (3.0.1): PDF text extraction using `PDFTextStripper`
- **Jsoup** (1.17.1): HTML parsing for extracting text from HTML content
- **Hutool** (5.8.23): Java utility library (all-in-one)
- **MyBatis Plus** (3.5.5): ORM (configured but unused in current implementation)
- **H2**: In-memory database (configured but unused)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/file/convert/doc-to-docx` | Convert .doc to .docx format |
| POST | `/api/file/extract/zip-index-html` | Extract index.html from ZIP to plain text |
| POST | `/api/file/extract/text` | Extract text from DOC/DOCX/PDF/PPT/PPTX |
| GET | `/api/file/info?path={path}` | Get file metadata |
| GET | `/api/file/health` | Health check |

All endpoints return `FileResponse` JSON with `success`, `message`, `filePath`, `fileSize` fields.

## File Processing Details

### DOC to DOCX Conversion
- Direct POI API conversion preserving formatting (bold, italic, underline, font size, color)
- Alternative HTML bridge method exists but is unused
- Handles paragraph and character run-level formatting

### ZIP HTML Extraction
- Case-insensitive search for "index.html" anywhere in the archive
- Strips script/style tags, converts headers/paragraphs/lists to formatted text
- Outputs UTF-8 text file

### Document Text Extraction
- Supports DOC, DOCX, PDF, PPT, PPTX formats via `TextExtractor.isSupportedFormat()`
- Uses `WordExtractor` for DOC, `XWPFDocument` for DOCX, `Loader.loadPDF()` for PDF
- PPT uses `HSLFSlideShow`, PPTX uses `XMLSlideShow` - extracts text per slide with slide markers
- DOCX preserves table content with cell separators
- Returns structured plain text, UTF-8 encoded

## Testing

Tests use `@SpringBootTest` for integration testing with the full context. Tests create temporary files in `./test-files/` and clean up after execution.
