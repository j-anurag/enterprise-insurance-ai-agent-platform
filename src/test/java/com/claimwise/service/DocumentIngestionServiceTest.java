package com.claimwise.service;

import com.claimwise.exception.InvalidRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIngestionServiceTest {

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionServiceImpl();
    }

    private byte[] createSamplePdf(String sampleText) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(sampleText);
                stream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Should extract text from valid PDF bytes")
    void shouldExtractTextFromValidPdfBytes() throws IOException {
        String content = "ClaimWise Insurance Coverage Details";
        byte[] pdfBytes = createSamplePdf(content);

        String extracted = ingestionService.extractText(pdfBytes);

        assertThat(extracted).contains("ClaimWise Insurance Coverage Details");
    }

    @Test
    @DisplayName("Should reject empty or null PDF bytes")
    void shouldRejectEmptyPdfBytes() {
        assertThatThrownBy(() -> ingestionService.extractText((byte[]) null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("content is empty");

        assertThatThrownBy(() -> ingestionService.extractText(new byte[0]))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("content is empty");
    }

    @Test
    @DisplayName("Should reject corrupted bytes")
    void shouldRejectCorruptedPdfBytes() {
        byte[] corrupt = "not a valid pdf".getBytes();
        assertThatThrownBy(() -> ingestionService.extractText(corrupt))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Failed to parse PDF");
    }

    @Test
    @DisplayName("Should reject empty text PDF")
    void shouldRejectEmptyPdfDocument() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            byte[] blankPdf = baos.toByteArray();

            assertThatThrownBy(() -> ingestionService.extractText(blankPdf))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("contains no readable text");
        }
    }

    @Test
    @DisplayName("Should verify supported document types and references")
    void shouldCheckSupportedTypes() {
        assertThat(ingestionService.isSupported("application/pdf", "policy.pdf")).isTrue();
        assertThat(ingestionService.isSupported(null, "policy.pdf")).isTrue();
        assertThat(ingestionService.isSupported("application/pdf", "policy.docx")).isTrue();
        assertThat(ingestionService.isSupported("text/plain", "policy.txt")).isFalse();
    }
}
