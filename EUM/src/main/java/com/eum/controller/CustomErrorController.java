package com.eum.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("javax.servlet.error.status_code");
        Object message = request.getAttribute("javax.servlet.error.message");
        Object exception = request.getAttribute("javax.servlet.error.exception");
        Object path = request.getAttribute("javax.servlet.error.request_uri");
        
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        model.addAttribute("exception", exception);
        model.addAttribute("path", path);
        
        System.out.println("=== ERROR DETAILS ===");
        System.out.println("Error Status: " + status);
        System.out.println("Error Message: " + message);
        System.out.println("Error Path: " + path);
        if (exception != null) {
            System.out.println("Error Exception: " + exception.toString());
            if (exception instanceof Exception) {
                ((Exception) exception).printStackTrace();
            }
        }
        System.out.println("===================");
        
        return "error";
    }
} 
