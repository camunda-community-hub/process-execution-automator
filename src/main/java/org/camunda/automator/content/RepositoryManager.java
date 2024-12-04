package org.camunda.automator.content;

import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class RepositoryManager {
    private final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);

    private Path repositoryPath;

    public void initializeRepository(String repositoryProposition) throws AutomatorException {
        if (repositoryProposition != null && !repositoryProposition.isEmpty()) {
            Path path = Paths.get(repositoryProposition);
            if (Files.exists(path) && Files.isDirectory(path)) {
                repositoryPath = path;
            }
        } else {
            // Not exist: create a subfolder
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));

            // Create a new folder in the temporary directory
            try {
                repositoryPath = Files.createDirectory(tempDir.resolve("repository"));
            } catch (Exception e) {
                logger.error("Can't create folder [{}]", tempDir.toAbsolutePath() + "/repository");
                throw new AutomatorException("Can't create folder[" + tempDir.toAbsolutePath() + "/repository]");
            }
        }
        logger.info("RepositoryManager: directory under [{}] ", repositoryPath.toAbsolutePath());

    }

    public Path addResource(Resource resource) throws IOException {
        Path targetPath = repositoryPath.resolve(resource.getFilename());
        Files.copy(resource.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("CopiedReource: [{}] tp [{}]", resource.getFilename(), targetPath);
        return targetPath;
    }

    public Path addFile(Path sourcePath) throws IOException {
        Path sourceFileName = sourcePath.getFileName();
        // Get the directory from targetPath
        Path targetDir = repositoryPath.getParent();
        // Combine the directory of targetPath with the filename of sourcePath
        Path targetPath = targetDir.resolve(sourceFileName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("CopiedFile: [{}] tp [{}]", sourcePath, targetPath);
        return targetPath;
    }

    public Path addFromInputStream(InputStream inputStream, String fileName) throws IOException {
        OutputStream outputStream = null;
        File fileContent = null;
        try {
            fileContent = new File(repositoryPath + File.separator + fileName);
            // Open an OutputStream to the temporary file
            outputStream = new FileOutputStream(fileContent);
            // Transfer data from InputStream to OutputStream
            byte[] buffer = new byte[1024 * 100]; // 100Ko
            int bytesRead;
            int count = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                count += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            outputStream = null;
            return fileContent.toPath();
        } catch (Exception e) {
            logger.error("RepositoryManager/addFromInputStream: Can't upload File [" + fileName + "] : " + e.getMessage());
            throw e;
        } finally {
            if (outputStream != null)
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // do nothing
                }
        }
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }

    public Path getFromName(String scenarioName) {
        Path scenarioPath = Paths.get(repositoryPath + File.separator + scenarioName + ".json");
        if (Files.exists(scenarioPath)) {
            return scenarioPath;
        }
        return null;
    }


    /**
     * Return the content of the repository path
     *
     * @return list of files
     */
    public List<Path> getContentRepository() {
        try (Stream<Path> files = Files.walk(repositoryPath)) {
            return files.filter(Files::isRegularFile)  // You can filter by file type if needed
                    .toList();
        } catch (IOException e) {
            logger.error("Error reading content [{}]", repositoryPath.toString());
            return Collections.emptyList();
        }
    }

    public File prepareFileToUpload(String fileName) {
        Path scenarioPath = Paths.get(repositoryPath + File.separator + fileName);
        return scenarioPath.toFile();
    }
}
