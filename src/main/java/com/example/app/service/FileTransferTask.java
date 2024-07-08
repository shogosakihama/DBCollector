package com.example.app.service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.smbj.common.SMBRuntimeException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.Callable;

public class FileTransferTask implements Callable<String> {
    private String sourceIpAddress;
    private String destinationIpAddress;
    private String sourceShareName;
    private String destinationShareName;
    private String sourceRelativePath;
    private String destinationRelativePath;
    private final String username = "developers";
    private final String password = "developers";

    public FileTransferTask(String sourceIpAddress, String destinationIpAddress, String sourceShareName,
                            String destinationShareName, String sourceRelativePath, String destinationRelativePath) {
        this.sourceIpAddress = sourceIpAddress;
        this.destinationIpAddress = destinationIpAddress;
        this.sourceShareName = sourceShareName;
        this.destinationShareName = destinationShareName;
        this.sourceRelativePath = sourceRelativePath;
        this.destinationRelativePath = destinationRelativePath;
    }

    @Override
    public String call() {
        try (SMBClient client = new SMBClient()) {
            System.out.println("Connecting to source IP: " + sourceIpAddress);
            try (Connection sourceConnection = client.connect(sourceIpAddress)) {
                AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), null);
                Session sourceSession = sourceConnection.authenticate(ac);
                DiskShare sourceShare = (DiskShare) sourceSession.connectShare(sourceShareName);

                String filePath = sourceRelativePath + "\\test1.log";
                System.out.println("Opening source file: " + filePath);
                try (File srcFile = sourceShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
                     java.io.InputStream in = srcFile.getInputStream()) {

                    long fileSize = srcFile.getFileInformation().getStandardInformation().getEndOfFile();
                    System.out.println("Source file size: " + fileSize);

                    System.out.println("Connecting to destination IP: " + destinationIpAddress);
                    try (Connection destConnection = client.connect(destinationIpAddress)) {
                        Session destSession = destConnection.authenticate(ac);
                        DiskShare destShare = (DiskShare) destSession.connectShare(destinationShareName);

                        String destinationFilePath = destinationRelativePath + "\\test1.log";
                        System.out.println("Opening destination file: " + destinationFilePath);
                        try (File destFile = destShare.openFile(destinationFilePath, EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null);
                             java.io.OutputStream out = destFile.getOutputStream()) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesRead = 0;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                            }
                            System.out.println("Read " + totalBytesRead + " bytes from " + filePath);
                            System.out.println("Wrote " + totalBytesRead + " bytes to " + destinationFilePath);
                        }
                    }
                }
            } catch (SMBRuntimeException e) {
                e.printStackTrace();
                return "File transfer failed due to SMB error: " + e.getMessage();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "File transfer failed due to IO error: " + e.getMessage();
        }
        return "File transfer successful";
    }
}
