package com.example.menu_pos.ui.checkout;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import java.io.FileOutputStream;
import java.io.IOException;

/** PrintDocumentAdapter that prints a provided PDF byte[]. */
public class PdfBytesPrintAdapter extends PrintDocumentAdapter {

    private final String documentName;
    private final byte[] pdfBytes;

    public PdfBytesPrintAdapter(String documentName, byte[] pdfBytes) {
        this.documentName = documentName;
        this.pdfBytes = pdfBytes;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }
        PrintDocumentInfo info = new PrintDocumentInfo.Builder(documentName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        try (FileOutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
            out.write(pdfBytes);
            out.flush();
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(e.getMessage());
        }
    }
}

