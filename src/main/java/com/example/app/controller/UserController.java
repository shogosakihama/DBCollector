package com.example.app.controller;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.repository.UserRepository;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMB2CreateDisposition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
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
        return "userForm"; // userForm.htmlを表示
    }

    @PostMapping("/user-details")
    public String getUserDetailsBySystemId(@RequestParam String userSystemId, Model model) {
        List<UserDetailsDTO> userDetails = repository.findUserDetailsByUserSystemId(userSystemId);
        model.addAttribute("userDetails", userDetails);
        return "userDetailsResult"; // userDetailsResult.htmlを表示
    }

    // @GetMapping("/disk-space")
    // public String getDiskSpace(Model model) {
    //     addDiskSpaceAttributes(model);
    //     return "diskSpace";
    // }

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
        String username = userDetail.getOptionUsername();
        String password = userDetail.getOptionPassword();

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

        try (SMBClient client = new SMBClient()) {
            try (Connection connection = client.connect(sourceIpAddress)) {
                username = "developers";
                password = "developers";
                AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), null);
                Session session = connection.authenticate(ac);
                DiskShare share = (DiskShare) session.connectShare(sourceShareName);

                for (FileIdBothDirectoryInformation file : share.list(sourceRelativePath, "*")) {
                    String fileName = file.getFileName();
                    if (fileName.equals(".") || fileName.equals("..")) {
                        continue; // .や..ディレクトリを除外
                    }
                    String filePath = sourceRelativePath + "\\" + fileName;
                    System.out.println("Found file: " + filePath);

                    try {
                        try (File srcFile = share.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null,SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
                             java.io.InputStream in = srcFile.getInputStream()) {

                            long fileSize = srcFile.getFileInformation().getStandardInformation().getEndOfFile();
                            System.out.println("File size: " + fileSize);

                            try (Connection destConnection = client.connect(destinationIpAddress)) {
                                Session destSession = destConnection.authenticate(ac);
                                DiskShare destShare = (DiskShare) destSession.connectShare(destinationShareName);

                                try (File destFile = destShare.openFile(destinationRelativePath + "\\" + fileName,
                                        EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null);
                                     java.io.OutputStream out = destFile.getOutputStream()) {

                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    long totalBytesRead = 0;
                                    while ((bytesRead = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;
                                    }
                                    System.out.println("Read " + totalBytesRead + " bytes from " + filePath);
                                    System.out.println("Wrote " + totalBytesRead + " bytes to "
                                            + destinationRelativePath + "\\" + fileName);
                                }
                            }
                        }
                    } catch (SMBApiException e) {
                        System.out.println("Failed to open file: " + filePath + " due to " + e.getStatusCode());
                        e.printStackTrace();
                        model.addAttribute("message", "File transfer failed: Unable to open file " + fileName
                                + " due to " + e.getStatusCode());
                        return "userDetailsResult";
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("message", "File transfer failed: " + e.getMessage());
            return "userDetailsResult";
        }

        model.addAttribute("message", "File transfer successful");
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
