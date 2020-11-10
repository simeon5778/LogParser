package com.company;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class GzUnzip {

    protected static File decompressGzip(String fileToUnzip, String unzippedFile) {
        Path fileToUnzipPath = Paths.get(fileToUnzip);
        Path unzippedFilePath = Path.of(unzippedFile);

        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(fileToUnzipPath.toFile()));
                FileOutputStream fos = new FileOutputStream(unzippedFilePath.toFile())) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return unzippedFilePath.toFile();
    }
}
