package com.claimwise.service;

import java.io.File;

public interface DocumentIngestionService {

    String extractText(String storageReference);

    String extractText(byte[] pdfBytes);

    String extractText(File file);

    boolean isSupported(String documentType, String storageReference);
}
