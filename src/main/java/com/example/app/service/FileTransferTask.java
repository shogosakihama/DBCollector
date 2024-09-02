package com.example.app.service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.IOException;
import java.util.EnumSet;

public class FileTransferTask implements Runnable {
    private String sourceIpAddress;
    private String destinationIpAddress;
    private String sourceShareName;
    private String destinationShareName;
    private String sourceRelativePath;
    private String destinationRelativePath;
    private String sourceUsername;
    private String sourcePassword;
    private String destUsername;
    private String destPassword;

    public FileTransferTask(String sourceIpAddress, String destinationIpAddress, String sourceShareName,
                            String destinationShareName, String sourceRelativePath, String destinationRelativePath,
                            String sourceUsername, String sourcePassword, String destUsername, String destPassword) {
        this.sourceIpAddress = sourceIpAddress;
        this.destinationIpAddress = destinationIpAddress;
        this.sourceShareName = sourceShareName;
        this.destinationShareName = destinationShareName;
        this.sourceRelativePath = sourceRelativePath;
        this.destinationRelativePath = destinationRelativePath;
        this.sourceUsername = sourceUsername;
        this.sourcePassword = sourcePassword;
        this.destUsername = destUsername;
        this.destPassword = destPassword;
    }

    @Override
    public void run() {
        try (SMBClient client = new SMBClient()) {
            // ソースサーバーへの接続
            System.out.println("Connecting to source server: " + sourceIpAddress);
            try (Connection sourceConnection = client.connect(sourceIpAddress)) {
                AuthenticationContext sourceAc = new AuthenticationContext(sourceUsername, sourcePassword.toCharArray(), null);
                Session sourceSession = sourceConnection.authenticate(sourceAc);
                DiskShare sourceShare = (DiskShare) sourceSession.connectShare(sourceShareName);

                String filePath = sourceRelativePath + "\\test1.log";
                System.out.println("Opening source file: " + filePath);
                try (File srcFile = sourceShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
                     java.io.InputStream in = srcFile.getInputStream()) {

                    long fileSize = srcFile.getFileInformation().getStandardInformation().getEndOfFile();
                    System.out.println("Source file size: " + fileSize);

                    // デスティネーションサーバーへの接続
                    System.out.println("Connecting to destination server: " + destinationIpAddress);
                    try (Connection destConnection = client.connect(destinationIpAddress)) {
                        AuthenticationContext destAc = new AuthenticationContext(destUsername, destPassword.toCharArray(), null);
                        System.out.println("destUsername: " + destUsername +"destPassword: " + destPassword);
                        Session destSession = destConnection.authenticate(destAc);
                        System.out.println("destSession: " + destSession);
                        DiskShare destShare = (DiskShare) destSession.connectShare(destinationShareName);
                        System.out.println("destinationShareName: " + destinationShareName);

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
                        } catch (IOException e) {
                            System.err.println("Failed to open or write to destination file: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to connect or authenticate to destination server: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to open or read from source file: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (IOException e) {
                System.err.println("Failed to connect or authenticate to source server: " + e.getMessage());
                e.printStackTrace();
            }
        } 
    }
}
