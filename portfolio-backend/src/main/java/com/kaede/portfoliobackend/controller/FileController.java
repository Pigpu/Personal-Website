package com.kaede.portfoliobackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;

@RestController
@RequestMapping("/api")
public class FileController {

    // 基础路径：保持你原来的设置不变
    @Value("${upload.path}")
    private String BASE_UPLOAD_PATH;
    // 基础 URL：用于拼接返回给前端的访问地址
    @Value("${upload.base-url}")
    private String BASE_URL;

    /**
     * 原有接口：保持不变 (供文章编辑器使用)
     * 路径: POST /api/upload
     * 参数: image (文件)
     * 返回: 纯字符串 URL
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) return "上传失败";
        try {
            // 为了保持兼容，老接口的文件继续存在 uploads 根目录下
            String fileName = saveFile(file, "");
            return BASE_URL + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "上传出错";
        }
    }

    /**
     * 新增接口：适配作品板块 (Project)
     * 路径: POST /api/upload/project
     * 参数: file (文件), type (类型字符串: cover, media, attachment)
     * 返回: JSON 格式的 URL (更规范)
     */
    @PostMapping("/upload/project")
    public ResponseEntity<?> uploadProjectFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {

        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        try {
            // 根据类型决定存放到哪个子文件夹
            // 结果会变成: uploads/covers/, uploads/media/, uploads/files/
            String subDir = switch (type) {
                case "cover" -> "covers/";
                case "media" -> "media/";      // 视频或音频
                case "attachment" -> "files/"; // 附件
                default -> "others/";
            };

            String fileName = saveFile(file, subDir);

            // 返回完整访问 URL
            return ResponseEntity.ok(BASE_URL + subDir + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("上传失败: " + e.getMessage());
        }
    }

    /**
     * 提取出的通用保存逻辑
     * @param file 文件对象
     * @param subDir 子目录 (例如 "media/")，如果是根目录传 ""
     * @return 最终保存的文件名
     */
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        // 1. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + suffix;

        // 2. 确保存储目录存在
        File dir = new File(BASE_UPLOAD_PATH + subDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 自动创建多级目录
        }

        // 3. 保存文件
        File dest = new File(BASE_UPLOAD_PATH + subDir + fileName);
        file.transferTo(dest);

        return fileName;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileUrl) {
        try {
            // 兼容绝对路径和相对路径
            String urlStr = fileUrl.startsWith("http") ? fileUrl : "http://localhost:8080" + fileUrl;
            URL url = new URL(urlStr);
            String path = url.getPath();

            // 提取出 uploads 后面的相对路径，比如 files/xxx.pdf
            String relativePath = path.substring(path.indexOf("/uploads/") + 9);

            // 拼接服务器本地路径
            Path filePath = Paths.get(BASE_UPLOAD_PATH, relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        // 🌟 核心：强制触发浏览器下载行为
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}