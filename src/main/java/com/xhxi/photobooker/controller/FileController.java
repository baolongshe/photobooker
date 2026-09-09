package com.xhxi.photobooker.controller;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.xhxi.photobooker.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    //腾讯云COS配置
    @Value("${cos.secret-id}")
    private String SECRET_ID;
    @Value("${cos.secret-key}")
    private String SECRET_KEY;
    @Value("${cos.region}")
    private String REGION ;
    @Value("${cos.bucket-name}")
    private String BUCKET ;

    //采用cos存储
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        COSClient cosClient = null;
        try {
            COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
            ClientConfig clientConfig = new ClientConfig(new Region(REGION));
            cosClient = new COSClient(cred, clientConfig);

            String filename = "avatar/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, filename, file.getInputStream(), new ObjectMetadata());
            cosClient.putObject(putObjectRequest);

            String url = "https://" + BUCKET + ".cos." + REGION + ".myqcloud.com/" + filename;
            return Result.success(url);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
    @PostMapping("/photo/upload")
    public Result<String> photoUpload(@RequestParam("file") MultipartFile file) throws IOException {
        COSClient cosClient = null;
        try {
            COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
            ClientConfig clientConfig = new ClientConfig(new Region(REGION));
            cosClient = new COSClient(cred, clientConfig);

            String filename = "photos/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, filename, file.getInputStream(), new ObjectMetadata());
            cosClient.putObject(putObjectRequest);

            String url = "https://" + BUCKET + ".cos." + REGION + ".myqcloud.com/" + filename;
            return Result.success(url);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
    /**
     * 批量上传照片
     */
    @PostMapping("/upload/batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        COSClient cosClient = null;
        try {
            COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
            ClientConfig clientConfig = new ClientConfig(new Region(REGION));
            cosClient = new COSClient(cred, clientConfig);

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                String filename = "photos/" + System.currentTimeMillis() + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
                PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, filename, file.getInputStream(), new ObjectMetadata());
                cosClient.putObject(putObjectRequest);

                String url = "https://" + BUCKET + ".cos." + REGION + ".myqcloud.com/" + filename;
                urls.add(url);
            }

            return Result.success(urls);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
} 