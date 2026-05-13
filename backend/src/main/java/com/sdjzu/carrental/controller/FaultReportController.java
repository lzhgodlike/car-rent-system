package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.request.FaultHandleRequest;
import com.sdjzu.carrental.model.request.FaultReportRequest;
import com.sdjzu.carrental.service.FaultReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fault-reports")
public class FaultReportController {

    private final FaultReportService faultReportService;

    public FaultReportController(FaultReportService faultReportService) {
        this.faultReportService = faultReportService;
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody FaultReportRequest request) {
        faultReportService.create(request);
        return ApiResponse.success("故障上报成功", null);
    }

    @GetMapping
    public ApiResponse<PageResult<FaultReport>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) Long carId,
                                                      @RequestParam(required = false) String status) {
        return ApiResponse.success(faultReportService.list(pageNum, pageSize, carId, status));
    }

    @PutMapping("/{id}/handle")
    public ApiResponse<Void> handle(@PathVariable Long id, @Valid @RequestBody FaultHandleRequest request) {
        faultReportService.handle(id, request);
        return ApiResponse.success("故障已进入维修状态", null);
    }

    @PutMapping("/{id}/complete-repair")
    public ApiResponse<Void> completeRepair(@PathVariable Long id, @RequestBody FaultHandleRequest request) {
        faultReportService.completeRepair(id, request);
        return ApiResponse.success("维修完成", null);
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody FaultHandleRequest request) {
        faultReportService.reject(id, request);
        return ApiResponse.success("故障报告已拒绝", null);
    }
}
