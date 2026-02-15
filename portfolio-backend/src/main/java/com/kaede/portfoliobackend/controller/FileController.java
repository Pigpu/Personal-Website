package com.kaede.portfoliobackend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class FileController {

    // 图片在服务器上的存放位置
    private final String uploadPath = "D:/HomePageProject/HomepageDB/uploads/";

    @PostMapping("/upload")
    public String upload(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) return "上传失败";

        // 生成唯一文件名，防止重名覆盖
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadPath + fileName);

        try {
            file.transferTo(dest);
            // 返回图片的访问 URL
            return "http://localhost:8080/uploads/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "上传出错";
        }
    }
}
