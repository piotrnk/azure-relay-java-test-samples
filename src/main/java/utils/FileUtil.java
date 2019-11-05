package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static String getFileAsText(String fileName) {
        String data = null;
        Path path = null;
        try {
            path = Paths.get(
                    FileUtil.class.getClassLoader().getResource(fileName).toURI());
            Stream<String> lines = Files.lines(path);
            data = lines.collect(Collectors.joining("\n"));
            lines.close();
            if (data == null) {
                throw new Exception(
                        "The file: connectionString.txt is empty. Add connection string to this "
                        + "file.");
            }else{
                logger.info("connection string: {} ", data);
            }
        } catch (Exception e) {
            logger.error("failed to load connection string", e);
        }
        return data;

    }
}
