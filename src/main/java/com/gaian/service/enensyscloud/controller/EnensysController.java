package com.gaian.service.enensyscloud.controller;

import static com.gaian.services.proxytargeting.service.ProxyTargetingService.decorateFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gaian.cdf.client.model.payload.Payload;
import com.gaian.services.ggs.ca.validation.PayloadValidator;
import com.gaian.services.ggs.ca.validation.PayloadValidatorImpl;
import com.gaian.services.nrt.lib.entity.NrtFile;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController()
public class EnensysController {

    @Autowired
    @Qualifier("seqExecutorService")
    private ExecutorService seqExecutorService;
    @Value("${timeout:60000}")
    private Long timeout;

//    @Value("${devices.ftp.clients}")
//    private String ftpUsers;

    @Value("#{${devices.ftp.clients}}")
    private Map<String, String> deviceFtpUsers;


    @PostMapping("/push")
    public void push(@RequestBody Payload payload) throws JsonProcessingException {
        PayloadValidator validator = new PayloadValidatorImpl();
        List<NrtFile> files = validator.validateFilePayload(payload);
        String[] devices = payload.getDeviceIds().split(",");
        files.parallelStream().forEach(file -> {

            file.setDecoratedFilename(
                decorateFile(file.getTenantId(), file.getFilename(), file.getGroupId(), null));
            seqExecutorService.execute(() -> {
                uploadWaitDelete(file, devices);
            });

        });
    }

    private void uploadWaitDelete(NrtFile file, String[] devices) {
        try {
            uploadsFileSequentially(file, devices);
            waitAndDeletePendingFiles(file.getDecoratedFilename(), file, devices);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadsFileSequentially(NrtFile file, String[] devices) throws Exception {

        String filename = file.getDecoratedFilename();
        String mimeType = file.getMimeType();
        log.info("New file to upload to Enensys : {}", filename);

//        if (heldMimeType.equalsIgnoreCase(mimeType)) {
//            triggerBA(file);
//        }

        Arrays.asList(devices).parallelStream().forEach(device -> {
            String ftp = deviceFtpUsers.get(device);
            log.info("device prepared {}", device);
            String[] ftpDetails = ftp.split(":");
            uploadContent(file, file.getDestinationPath(), ftpDetails[0], ftpDetails[1],
                ftpDetails[2]);
        });
    }

    private void uploadContent(NrtFile file, String destinationPath, String ftpAddress,
        String ftpUser, String ftpPass) {
        FTPClient conn = null;
        boolean isUploadSuccess = false;
        try {
            conn = new FTPClient();
            InetAddress address = InetAddress.getByName(ftpAddress);
            conn.connect(address);
            if (conn.login(ftpUser, ftpPass)) {
                conn.enterLocalPassiveMode();
                conn.changeToParentDirectory();
                conn.setFileType(FTP.BINARY_FILE_TYPE);
                isUploadSuccess = conn.storeFile(file.getDecoratedFilename(),
                    new FileSystemResource(file.getPath() + "/" + file.getFilename())
                        .getInputStream());
                conn.logout();
                conn.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("FILE UPLOAD REQUEST TO ENENSYS Success:: {} ", isUploadSuccess);
    }

    private void waitAndDeletePendingFiles(String heldFilename, NrtFile file,
        String[] devices) throws
        IOException {
        log.info(">>>>>>>>>>>>>>> file upload waiting to purge .....");
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Arrays.asList(devices).parallelStream().forEach(device -> {
            String ftp = deviceFtpUsers.get(device);
            String[] ftpDetails = ftp.split(":");
            removeFileFromFtp(heldFilename, ftpDetails[0], ftpDetails[1], ftpDetails[2]);
        });
    }

    public void removeFileFromFtp(String fileName, String ftpAddress,
        String ftpUser, String ftpPass) {

        FTPClient conn = null;
        boolean isUploadSuccess = false;
        try {
            conn = new FTPClient();
            InetAddress address = InetAddress.getByName(ftpAddress);
            conn.connect(address);
            if (conn.login(ftpUser, ftpPass)) {
                conn.enterLocalPassiveMode();
                conn.changeToParentDirectory();
                conn.setFileType(FTP.BINARY_FILE_TYPE);
                conn.deleteFile(fileName);
                conn.disconnect();
                log.info(">>>>>>>>>>>>>>> file {} purged .....", fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
