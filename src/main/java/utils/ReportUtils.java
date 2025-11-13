package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class ReportUtils {
    private ReportUtils() {

    }

    public static void DeleteAllureFolder() throws IOException {
        deleteFolderContents(Paths.get("./target/allure-results"));
        deleteFolderContents(Paths.get("./test-recordings"));
        deleteFolderContents(Paths.get("./test-screenshots"));
    }
    private static void deleteFolderContents(Path folderPath) throws IOException {
        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            // Duyệt toàn bộ nội dung bên trong thư mục gốc (bao gồm file + thư mục con)
            Files.walk(folderPath)
                    .filter(path -> !path.equals(folderPath)) // bỏ qua thư mục gốc
                    .sorted(Comparator.reverseOrder())        // xóa file trước thư mục
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
