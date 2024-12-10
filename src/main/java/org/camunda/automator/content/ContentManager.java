package org.camunda.automator.content;

import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
@PropertySource("classpath:application.yaml")
@Configuration
public class ContentManager {

    private static final Logger logger = LoggerFactory.getLogger(ContentManager.class.getName());
    @Value("${automator.content.repositoryPath}")
    public String repositoryPath;
    @Value("${automator.content.uploadPath}")
    public String uploadPath;
    @Autowired
    ConfigurationStartup configurationStartup;
    RepositoryManager repositoryManager = new RepositoryManager();
    @Value("${automator.content.scenario:}")
    private Resource scenarioResource;

    public Path getFromName(String scenarioName) {
        return repositoryManager.getFromName(scenarioName);
    }


    @PostConstruct
    public void init() {
        try {
            logger.info("ContentManager: start initialization");
            repositoryManager.initializeRepository(repositoryPath);
            loadUploadPath();
            LoadContentResource();
        } catch (Exception e) {
            logger.error("ContentManager: error during initialization {}", e.getMessage(),e);
        }
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Repository management                                               */
    /*                                                                      */
    /* ******************************************************************** */

    public List<Path> getContent() {
        return repositoryManager.getContentRepository();
    }

    public List<Scenario> getContentScenario() {
        List<Scenario> listScenario = new ArrayList<>();
        List<Path> listContent = repositoryManager.getContentRepository();
        for (Path path : listContent) {
            // The content can have multiple files, not only scenario
            if (!path.getFileName().toString().endsWith(".json"))
                continue;
            try {
                Scenario scenario = Scenario.createFromFile(path);
                listScenario.add(scenario);
            } catch (AutomatorException e) {
                logger.error("ContentManager/getContentScenario: path [{}] failed: {}", path.getFileName(), e.getMessage());
            }
        }
        return listScenario;
    }


    public Path addFile(Path file) throws IOException {
        return repositoryManager.addFile(file);
    }

    public Path addResource(Resource resource) throws IOException {
        return repositoryManager.addResource(resource);
    }

    public Path addFromMultipart(MultipartFile file, String fileName) throws IOException {
        // save the file on a temporary disk
        return repositoryManager.addFromInputStream(file.getInputStream(), fileName);
    }
    /* ******************************************************************** */
    /*                                                                      */
    /*  Load management                                                     */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * Load from the content resource. This is typicaly provided on a Pod
     */
    private void LoadContentResource() {
        if (scenarioResource == null) {
            logger.info("ContentManager/LoadContentResource: No scenario resource");
            return;
        }
        try {
            logger.info("ContentManager/LoadContentResource: Detect [Resource] name[{}]", scenarioResource.getFilename());
            Path scenario = repositoryManager.addResource(scenarioResource);
        } catch (IOException e) {
            logger.error("ContentManager/LoadContentResource: Error occurred: {} ", e.getMessage(),e);
        }
    }

    /**
     * Upload from a path. This is typicaly provided on a local machine
     */
    private void loadUploadPath() {
        try {
            Path sourceDirectory = Paths.get(uploadPath);
            logger.info("ContentManager/Upload: from [{}]", sourceDirectory.toAbsolutePath());
            int nbFilesCopied = 0;
            // Copy all files from source to target
            List<Path> listFiles = Files.walk(sourceDirectory)
                    .filter(Files::isRegularFile).toList();

            for (Path sourcePath : listFiles)// Filter only regular files
            {
                try {
                    repositoryManager.addFile(sourcePath);
                    nbFilesCopied++;
                } catch (IOException e) {
                    logger.error("ContentManager/Upload: Error copying[{}] -> [{}] : {}", sourcePath, repositoryManager.getRepositoryPath(), e.getMessage(),e);
                }
            }
            logger.info("ContentManager/Upload: upload {} files", nbFilesCopied);

        } catch (IOException e) {
            logger.error("ContentManager/Upload: Error occurred: {} ", e.getMessage(),e);
        }
    }
}
