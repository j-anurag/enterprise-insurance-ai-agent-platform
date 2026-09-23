package com.claimwise.service;

import com.claimwise.exception.InvalidRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    @Override
    public boolean isSupported(String documentType, String storageReference) {
        if (documentType != null && documentType.equalsIgnoreCase("application/pdf")) {
            return true;
        }
        return storageReference != null && storageReference.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractText(String storageReference) {
        if (storageReference == null || storageReference.trim().isEmpty()) {
            throw new InvalidRequestException("Storage reference cannot be empty");
        }

        if (!storageReference.toLowerCase().endsWith(".pdf")) {
            throw new InvalidRequestException("Unsupported document format. Only PDF documents are supported: " + storageReference);
        }

        Path path = Paths.get(storageReference);
        if (!Files.exists(path)) {
            throw new InvalidRequestException("Document file not found at storage reference: " + storageReference);
        }

        return extractText(path.toFile());
    }

    @Override
    public String extractText(File file) {
        if (file == null || !file.exists()) {
            throw new InvalidRequestException("PDF file does not exist");
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            return extractTextFromDocument(document);
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to read or parse PDF document: " + e.getMessage());
        }
    }

    @Override
    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidRequestException("PDF document content is empty");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return extractTextFromDocument(document);
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to parse PDF bytes: " + e.getMessage());
        }
    }

    private String extractTextFromDocument(PDDocument document) throws IOException {
        if (document.isEncrypted()) {
            throw new InvalidRequestException("Encrypted PDF documents are not supported");
        }

        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);

        if (text == null || text.trim().isEmpty()) {
            throw new InvalidRequestException("PDF document contains no readable text");
        }

        return text.trim();
    }
}
