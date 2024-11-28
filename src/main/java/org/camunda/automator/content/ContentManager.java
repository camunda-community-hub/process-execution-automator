package org.camunda.automator.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Component
@PropertySource("classpath:application.yaml")
@Configuration

public class ContentManager {
    private static final Logger logger = LoggerFactory.getLogger(ContentManager.class.getName());
    @Value("${automator.content.repositoryPath}")
    public String repositoryPath;
    @Value("${automator.content.uploadPath}")
    public String uploadPath;

    public File getFromName(String scenarioName) {
        return new File(repositoryPath + File.separator + scenarioName + ".json");
    }


    public void saveFromMultiPartFile(MultipartFile file, String fileName) {
        // save the file on a temporary disk
        OutputStream outputStream = null;
        File fileScenario = null;
        try {
            fileScenario = new File(repositoryPath + File.separator + fileName);
            // Open an OutputStream to the temporary file
            outputStream = new FileOutputStream(fileScenario);
            // Transfer data from InputStream to OutputStream
            byte[] buffer = new byte[1024 * 100]; // 100Ko
            int bytesRead;
            int count = 0;
            InputStream inputStream = file.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                count += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            logger.error("Can't load File [" + fileName + "] : " + e.getMessage());
        } finally {
            if (outputStream != null)
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // do nothing
                }
        }
    }

    @PostConstruct
    public void init() {
        Path sourceDirectory = Paths.get(uploadPath);
        Path targetDirectory = Paths.get(repositoryPath);
        logger.info("ContentManager initiaalisation Copied: [{}] to [{}]", sourceDirectory, targetDirectory);

        int nbFilesCopied = 0;
        try {
            // Create target directory if it doesn't exist
            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            // Copy all files from source to target
            List<Path> listFiles = Files.walk(sourceDirectory)
                    .filter(Files::isRegularFile).toList();

            for (Path sourcePath : listFiles)// Filter only regular files
            {
                try {
                    Path targetPath = targetDirectory.resolve(sourceDirectory.relativize(sourcePath));
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Copied: [{}] tp [{}]", sourcePath, targetPath);
                    nbFilesCopied++;
                } catch (IOException e) {
                    logger.error("Error copying file: [{}] ->  [{}] : {}", sourcePath, targetDirectory, e.getMessage());
                }
            }


        } catch (IOException e) {
            logger.error("Error occurred: {} ", e.getMessage());
        }
        logger.info("End of ContentManager {} files copied", nbFilesCopied);
    }

}
