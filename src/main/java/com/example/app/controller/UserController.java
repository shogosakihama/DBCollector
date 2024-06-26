package com.example.app.controller;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Controller
public class UserController {

    private final UserRepository repository;

    @Autowired
    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/form")
    public String showForm(Model model) {
        addDiskSpaceAttributes(model);
        return "userForm"; // userForm.htmlを表示
    }

    @PostMapping("/user-details")
    public String getUserDetailsBySystemId(@RequestParam String userSystemId, Model model) {
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        model.addAttribute("userDetails", userDetails);
        addDiskSpaceAttributes(model);
        return "userDetailsResult"; // userDetailsResult.htmlを表示
    }

    @GetMapping("/disk-space")
    public String getDiskSpace(Model model) {
        addDiskSpaceAttributes(model);
        return "diskSpace";
    }

    @PostMapping("/view-files")
    public String viewFilesInPath(@RequestParam String path, Model model) {
        File folder = new File(path);
        List<String> files = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                files.add(file.getName());
            }
        }
        model.addAttribute("files", files);
        return "userDetailsResult"; // userDetailsResult.htmlにリダイレクト
    }

    private void addDiskSpaceAttributes(Model model) {
        File cDrive = new File("C:");

        long totalSpace = cDrive.getTotalSpace();
        long freeSpace = cDrive.getFreeSpace();
        long usableSpace = cDrive.getUsableSpace();

        model.addAttribute("totalSpace", String.format("%.2f", totalSpace / 1e9));
        model.addAttribute("freeSpace", String.format("%.2f", freeSpace / 1e9));
        model.addAttribute("usableSpace", String.format("%.2f", usableSpace / 1e9));
    }
}
