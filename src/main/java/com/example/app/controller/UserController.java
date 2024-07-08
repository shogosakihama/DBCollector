package com.example.app.controller;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.repository.UserRepository;
import com.example.app.service.FileTransferTask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.common.SMBRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Controller
public class UserController {

    private final UserRepository repository;
    private final ExecutorService executorService;
    private final String username = "developers";
    private final String password = "developers";

    @Autowired
    public UserController(UserRepository repository) {
        this.repository = repository;
        this.executorService = Executors.newFixedThreadPool(10); // 10スレッドのスレッドプールを作成
    }

    @GetMapping("/form")
    public String showForm(Model model) {
        return "userForm"; // userForm.htmlを表示
    }

    @PostMapping("/user-details")
    public String getUserDetailsBySystemId(@RequestParam String userSystemId, Model model) {
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        model.addAttribute("userDetails", userDetails);
        return "userDetailsResult"; // userDetailsResult.htmlを表示
    }

    @PostMapping("/view-files")
    public String viewFilesInPath(@RequestParam String userSystemId, Model model) {
        if (userSystemId == null || userSystemId.isEmpty()) {
            model.addAttribute("message", "User System ID is required.");
            return "userDetailsResult";
        }

        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        if (userDetails.isEmpty()) {
            model.addAttribute("files", new ArrayList<>());
            model.addAttribute("userDetails", userDetails);
            return "userDetailsResult";
        }

        UserDetailsDTO userDetail = userDetails.get(0);
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
                    String fileName = file.getFileName();
                    if (!fileName.equals(".") && !fileName.equals("..")) {
                        String filePath = folderName + "\\" + fileName;
                        System.out.println("Found file: " + filePath);
                        files.add(fileName);
                    }
                }
            } catch (SMBRuntimeException e) {
                System.err.println("SMB Error: " + e.getMessage());
                model.addAttribute("message", "Error accessing shared folder: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
            model.addAttribute("message", "Error accessing SMB client: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("files", files);
        model.addAttribute("userDetails", userDetails);
        return "userDetailsResult"; // userDetailsResult.htmlにリダイレクト
    }

    @PostMapping("/transfer-files")
    public String transferFiles(@RequestParam String sourcePath, @RequestParam String destinationPath,
                                @RequestParam String userSystemId, Model model) {
        System.out.println("Starting file transfer from " + sourcePath + " to " + destinationPath + " for userSystemId: " + userSystemId);
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        if (userDetails.isEmpty()) {
            model.addAttribute("files", new ArrayList<>());
            System.out.println("User details not found for userSystemId: " + userSystemId);
            return "userDetailsResult";
        }

        UserDetailsDTO userDetail = userDetails.get(0);

        String sourceIpAddress = extractIpAddress(sourcePath);
        String destinationIpAddress = extractIpAddress(destinationPath);
        String sourceShareName = extractShareName(sourcePath);
        String destinationShareName = extractShareName(destinationPath);
        String sourceRelativePath = extractRelativePath(sourcePath, sourceIpAddress, sourceShareName);
        String destinationRelativePath = extractRelativePath(destinationPath, destinationIpAddress, destinationShareName);

        System.out.println("Source IP Address: " + sourceIpAddress);
        System.out.println("Destination IP Address: " + destinationIpAddress);
        System.out.println("Source Share Name: " + sourceShareName);
        System.out.println("Destination Share Name: " + destinationShareName);
        System.out.println("Source Relative Path: " + sourceRelativePath);
        System.out.println("Destination Relative Path: " + destinationRelativePath);

        FileTransferTask task = new FileTransferTask(sourceIpAddress, destinationIpAddress, sourceShareName,
                destinationShareName, sourceRelativePath, destinationRelativePath);
        executorService.submit(task);

        model.addAttribute("message", "File transfer started in the background.");
        model.addAttribute("userDetails", userDetails);
        return "userDetailsResult";
    }

    private String extractIpAddress(String path) {
        String[] parts = path.split("\\\\");
        return parts.length > 2 ? parts[2] : "";
    }

    private String extractShareName(String path) {
        String[] parts = path.split("\\\\");
        return parts.length > 3 ? parts[3] : "";
    }

    private String extractRelativePath(String path, String ipAddress, String shareName) {
        String prefix = "\\\\" + ipAddress + "\\" + shareName + "\\";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    @PreDestroy
    public void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("ExecutorService did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
