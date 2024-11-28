package org.camunda.automator.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ContentRestController {
    private static final Logger logger = LoggerFactory.getLogger(ContentRestController.class.getName());


    @Autowired
    ContentManager contentManager;

    @PostMapping(value = "/api/content/add", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> upload(@RequestPart("File") List<MultipartFile> uploadedfiles) {
        Map<String, Object> status = new HashMap<>();
        for (MultipartFile file : uploadedfiles) {
            String resultFile = "Load [" + file.getName() + "]";

            // is this worker is running?
            String jarFileName = file.getOriginalFilename();
            contentManager.saveFromMultiPartFile(file, jarFileName);

        }
        return new HashMap<>();
    }


}