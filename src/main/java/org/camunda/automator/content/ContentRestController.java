package org.camunda.automator.content;

import org.camunda.automator.definition.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ContentRestController {
    private static final Logger logger = LoggerFactory.getLogger(ContentRestController.class.getName());


    @Autowired
    ContentManager contentManager;

    /**
     * curl -X POST "http://localhost:8381/api/content/add" -H "Content-Type: multipart/form-data" -F "File=@C:/dev/intellij/community/process-execution-automator/doc/unittestscenario/resources/ScoreAcceptanceScn.json"
     **/
    @PostMapping(value = "/api/content/add", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> upload(@RequestPart("File") List<MultipartFile> uploadedfiles) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MultipartFile file : uploadedfiles) {
            try {
                Path fileSaved = contentManager.addFromMultipart(file, file.getOriginalFilename());
                result.add(Map.of("filename", fileSaved.getFileName(), "status", "UPLOADED"));
            } catch (Exception e) {
                result.add(Map.of("filename", file.getOriginalFilename(), "status", "ERROR", "error", e.getMessage()));

            }
        }
        return result;
    }


    @GetMapping("/api/content/list")
    List<Map<String, Object>> getContentScenario() {
        try {
            return contentManager.getContentScenario().stream()
                    .map(Scenario::getDescription)
                    .toList();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error during Content : " + e.getMessage());
        }
    }

}