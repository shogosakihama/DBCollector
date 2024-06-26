package com.example.app.controller;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.repository.UserRepository;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.auth.AuthenticationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
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
    public String viewFilesInPath(@RequestParam String userSystemId, Model model) {
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        if (userDetails.isEmpty()) {
            model.addAttribute("files", new ArrayList<>());
            return "userDetailsResult";
        }

        UserDetailsDTO userDetail = userDetails.get(0);
        String username = userDetail.getOptionUsername();
        String password = userDetail.getOptionPassword();
        String shareName = userDetail.getShareName();
        String folderName = userDetail.getFolderName();

        System.out.println("Share Name: " + shareName);
        System.out.println("Folder Name: " + folderName);

        List<String> files = new ArrayList<>();
        try (SMBClient client = new SMBClient()) {
            try (Connection connection = client.connect(userDetail.getIpAddress())) {
                AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), null);
                Session session = connection.authenticate(ac);
                DiskShare share = (DiskShare) session.connectShare(shareName); 
                for (FileIdBothDirectoryInformation file : share.list(folderName, "*")) {
                    files.add(file.getFileName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.addAttribute("files", files);
        model.addAttribute("userDetails", userDetails);
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
