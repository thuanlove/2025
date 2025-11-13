package utils;

import org.monte.screenrecorder.ScreenRecorder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SpecializedScreenRecorder extends ScreenRecorder {
    private String name;
    private File recordedFile;
    public SpecializedScreenRecorder(GraphicsConfiguration cfg,
                                     Rectangle captureArea,
                                     org.monte.media.Format fileFormat,
                                     org.monte.media.Format screenFormat,
                                     org.monte.media.Format mouseFormat,
                                     org.monte.media.Format audioFormat,
                                     File movieFolder,
                                     String name) throws IOException, AWTException {
        super(cfg, captureArea, fileFormat, screenFormat, mouseFormat, audioFormat, movieFolder);
        this.name = name;
    }

    @Override
    protected File createMovieFile(org.monte.media.Format fileFormat) throws IOException {
        // Create folder name in format: YYYYMMDD-hhmmss
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        movieFolder = new File("test-recordings/" + timeStamp);

        if (!movieFolder.exists()) {
            movieFolder.mkdirs();
        }
        recordedFile = new File(movieFolder, name + "_" + timeStamp + ".avi");
        return recordedFile;
    }
    public File getRecordedFile() {
        return recordedFile;
    }
}
