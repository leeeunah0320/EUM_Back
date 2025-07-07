package com.eum.controller;

import com.eum.service.RekognitionDetectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RekognitionTestController {
    private final RekognitionDetectService detectService;

    public RekognitionTestController(RekognitionDetectService detectService) {
        this.detectService = detectService;
    }

    @GetMapping("/rekognition/test")
    public String test() {
        return detectService.detectCustomLabel();
    }
}
