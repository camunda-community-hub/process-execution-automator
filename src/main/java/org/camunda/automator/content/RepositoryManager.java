package org.camunda.automator.content;

import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class RepositoryManager {
    private final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);

    private Path repositoryPath;

    public void initializeRepository(String repositoryProposition) throws AutomatorException {
        logger.info("RepositoryManager: initialisation proposition: {}", repositoryProposition);

        repositoryPath = null;
        if (repositoryProposition != null && !repositoryProposition.isEmpty()) {
            Path path = Paths.get(repositoryProposition);
            if (Files.exists(path) && Files.isDirectory(path)) {
                repositoryPath = path;
                logger.info("RepositoryManager: PathFromConfiguration [{}]", repositoryPath.toAbsolutePath());
            }
        }
        if (repositoryPath == null) {
            // Not exist: create a subfolder
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            logger.info("RepositoryManager/initialization: TemporaryFolder [{}]", tempDir.toAbsolutePath());

            // Create a new folder in the temporary directory
            try {
                repositoryPath = tempDir.resolve("repository");
                Files.createDirectory(repositoryPath);
                logger.info("RepositoryManager/initialization: PathFromTemporaryFolder [{}]", repositoryPath.toAbsolutePath());
            } catch (FileAlreadyExistsException e) {
                logger.info("RepositoryManager/initialization: File already exists [{}]", repositoryPath.toAbsolutePath());
            } catch (Exception e) {
                logger.error("RepositoryManager/initialization: Can't create folder [{}]", repositoryPath.toAbsolutePath());
                repositoryPath = null;
                throw new AutomatorException("Can't create folder[" + tempDir.toAbsolutePath() + "/repository]");
            }
        }
        logger.info("RepositoryManager/initialization: Folder [{}]", repositoryPath.toAbsolutePath());
    }

    public Path addResource(Resource resource) throws IOException {
        if (resource==null)
            return null;
        Path targetPath = repositoryPath.resolve(resource.getFilename());
        Files.copy(resource.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("CopiedResource: [{}] to [{}]", resource.getFilename(), targetPath);
        return targetPath;
    }

    public Path addFile(Path sourcePath) throws IOException {
        Path sourceFileName = sourcePath.getFileName();
        // Get the directory from targetPath
        // Combine the directory of targetPath with the filename of sourcePath
        Path targetPath = repositoryPath.resolve(sourceFileName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("CopiedFile: [{}] to [{}]", sourcePath, targetPath);
        return targetPath;
    }

    public Path addFromInputStream(InputStream inputStream, String fileName) throws IOException {
        OutputStream outputStream = null;
        try {
            File fileContent = new File(repositoryPath + File.separator + fileName);
            // Open an OutputStream to the temporary file
            outputStream = new FileOutputStream(fileContent);
            // Transfer data from InputStream to OutputStream
            byte[] buffer = new byte[1024 * 100]; // 100Ko
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
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
