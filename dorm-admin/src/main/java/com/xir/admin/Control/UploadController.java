package com.xir.admin.Control;

import com.xir.admin.Common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String base64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            return Result.success(base64);
        } catch (IOException e) {
            return Result.error("上传失败");
        }
    }
}