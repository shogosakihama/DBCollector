package com.example.app.controller;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.repository.UserRepository;
import com.example.app.service.FileTransferTask;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class UserController {

    private final UserRepository repository;
    private final ExecutorService executorService;

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
                    String fileName = file.getFileName();
                    if (!fileName.equals(".") && !fileName.equals("..")) {
                        String filePath = folderName + "\\" + fileName;
                        System.out.println("Found file: " + filePath);
                        files.add(fileName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.addAttribute("files", files);
        model.addAttribute("userDetails", userDetails);
        return "userDetailsResult"; // userDetailsResult.htmlにリダイレクト
    }

    @PostMapping("/transfer-files")
    public String transferFiles(@RequestParam String sourcePath, @RequestParam String destinationPath,
                                @RequestParam String userSystemId, Model model) {
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        if (userDetails.isEmpty()) {
            model.addAttribute("files", new ArrayList<>());
            return "userDetailsResult";
        }

        UserDetailsDTO userDetail = userDetails.get(0);
        String sourceUsername = "xxxx";
        String sourcePassword = "xxxx";
        String destUsername = "xxxx";
        String destPassword = "xxxx";

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

        // ファイル転送タスクをバックグラウンドで実行
        FileTransferTask task = new FileTransferTask(sourceIpAddress, destinationIpAddress, sourceShareName,
                destinationShareName, sourceRelativePath, destinationRelativePath, sourceUsername, sourcePassword, destUsername, destPassword);
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

    // private void addDiskSpaceAttributes(Model model) {
    //     File cDrive = new File("C:");

    //     long totalSpace = cDrive.getTotalSpace();
    //     long freeSpace = cDrive.getFreeSpace();
    //     long usableSpace = cDrive.getUsableSpace();

    //     model.addAttribute("totalSpace", String.format("%.2f", totalSpace / 1e9));
    //     model.addAttribute("freeSpace", String.format("%.2f", freeSpace / 1e9));
    //     model.addAttribute("usableSpace", String.format("%.2f", usableSpace / 1e9));
    // }

}
