package org.camunda.automator.api;

import org.camunda.automator.content.ContentManager;
import org.camunda.automator.definition.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("pea")

public class ContentRestController {
    private static final Logger logger = LoggerFactory.getLogger(ContentRestController.class.getName());


    @Autowired
    ContentManager contentManager;

    /**
     * curl -X POST "http://localhost:8381/api/content/add" -H "Content-Type: multipart/form-data" -F "File=@C:/dev/intellij/community/process-execution-automator/doc/unittestscenario/resources/ScoreAcceptanceScn.json"
     **/
    @PostMapping(value = "/api/content/add", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> upload(@RequestPart("scenarioFiles") List<MultipartFile> scenarioFiles) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MultipartFile file : scenarioFiles) {
            try {
                Path fileSaved = contentManager.addFromMultipart(file, file.getOriginalFilename());
                result.add(Map.of("filename", fileSaved.getFileName(), "status", "UPLOADED"));
                logger.info("ControlRestController: uploaded file[{}] with success", file.getOriginalFilename());
            } catch (Exception e) {
                logger.info("ControlRestController: Errir upload file [{}] : {}", file.getOriginalFilename(), e.getMessage());
                result.add(Map.of("filename", file.getOriginalFilename(), "status", "ERROR", "error", e.getMessage()));
            }
        }
        return result;
    }


    @GetMapping("/api/content/list")
    List<Map<String, Object>> getContentScenario(@RequestParam(name = "details", required = false) Boolean details) {
        logger.debug("ControlRestController/getContentScenario: start");
        try {
            List<Scenario> listScenario = contentManager.getContentScenario();
            List<Map<String, Object>> listScenarioMap = listScenario.stream()
                    .map(t -> t.getJson(details != null && details))
                    .toList();
            logger.info("ControlRestController/getContentScenario: found {} scenario : {]",
                    listScenario.size(),
                    listScenario.stream()
                            .map(Scenario::getName)
                            .collect(Collectors.joining(", "))
            );
            return listScenarioMap;
        } catch (Exception e) {
            logger.info("ControlRestController/getContentScenario: Error during getContentScenario {} ", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error during Content : " + e.getMessage());
        }
    }

    @PutMapping("/api/content/clearall")
    public void clearAllScenarii() {
        logger.debug("ControlRestController/clearAllScenarii: start");
        try {
            contentManager.clearAll();
        } catch (Exception e) {
            logger.info("ControlRestController/clearAllScenarii: Error during clear {} ", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error during Content : " + e.getMessage());
        }
    }

}