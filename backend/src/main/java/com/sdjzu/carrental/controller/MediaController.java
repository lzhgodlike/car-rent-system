package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.model.request.MediaImportRequest;
import com.sdjzu.carrental.model.vo.MediaFileVO;
import com.sdjzu.carrental.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ApiResponse<MediaFileVO> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "carNo", required = false) String carNo) {
        return ApiResponse.success("上传成功", mediaService.uploadCarImage(file, carNo));
    }

    @PostMapping("/import-by-url")
    public ApiResponse<MediaFileVO> importByUrl(@Valid @RequestBody MediaImportRequest request) {
        return ApiResponse.success("导入成功", mediaService.importCarImage(request.getUrl(), request.getCarNo()));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam String url) {
        mediaService.deleteCarImage(url);
        return ApiResponse.success("删除成功", null);
    }
}
