/*
 * 物联网的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/iot/hardware、/iot/hardware/devices、/iot/labs/{labId:\\d+}/devices、/iot/devices、/iot/labs/{labId:\\d+}/overview；完整路径由类级与方法级映射组合。
 * 主要协作组件：IotHardwareService、IotDeviceService。
 */
package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.service.IotDeviceService;
import org.example.backend.service.IotHardwareService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@CrossOrigin
@RestController
public class IotHardwareController {
    private final IotHardwareService iotHardwareService;
    private final IotDeviceService iotDeviceService;

    /**
     * 构造 IotHardwareController，初始化当前实例。
     * @param iotHardwareService 物联Hardware服务参数。
     * @param iotDeviceService 物联设备服务参数。
     */
    public IotHardwareController(IotHardwareService iotHardwareService, IotDeviceService iotDeviceService) {
        this.iotHardwareService = iotHardwareService;
        this.iotDeviceService = iotDeviceService;
    }

    /**
     * 获取Hardware概况。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/hardware")
    public Result getHardwareOverview() {
        return Result.success("获取物联网硬件概览成功", iotHardwareService.getOverview());
    }

    /**
     * 列出Hardware设备。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/hardware/devices")
    public Result listHardwareDevices() {
        return Result.success("获取物联网设备列表成功", iotHardwareService.listHardwareDevices());
    }

    /**
     * 列出实验室设备。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/labs/{labId:\\d+}/devices")
    public Result listLabDevices(@PathVariable Long labId) {
        return Result.success("获取实验室物联设备成功", iotDeviceService.listLabDevices(labId));
    }

    /**
     * 列出物联设备。
     * @param query 查询参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/devices")
    public Result listIotDevices(@RequestParam(required = false) Map<String, String> query) {
        return Result.success("获取物联设备列表成功", iotDeviceService.listDevices(query == null ? Map.of() : query));
    }

    /**
     * 获取实验室物联概况。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/labs/{labId:\\d+}/overview")
    public Result getLabIotOverview(@PathVariable Long labId) {
        return Result.success("获取实验室物联概览成功", iotDeviceService.getLabOverview(labId));
    }

    /**
     * 创建物联设备。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/iot/devices")
    public Result createIotDevice(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("新增物联设备成功", iotDeviceService.createDevice(payload == null ? Map.of() : payload));
    }

    /**
     * 更新物联设备。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/iot/devices/{id:\\d+}")
    public Result updateIotDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("更新物联设备成功", iotDeviceService.updateDevice(id, payload == null ? Map.of() : payload));
    }

    /**
     * 删除物联设备。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/iot/devices/{id:\\d+}")
    public Result deleteIotDevice(@PathVariable Long id) {
        iotDeviceService.deleteDevice(id);
        return Result.success("删除物联设备成功");
    }

    /**
     * control物联设备的处理逻辑。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/iot/control")
    public Result controlIotDevice(@RequestBody(required = false) Map<String, Object> payload) {
        return Result.success("物联控制命令已记录", iotDeviceService.control(payload == null ? Map.of() : payload));
    }

    /**
     * 列出命令Logs。
     * @param query 查询参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/command-logs")
    public Result listCommandLogs(@RequestParam(required = false) Map<String, String> query) {
        return Result.success("获取物联控制日志成功", iotDeviceService.listCommandLogs(query == null ? Map.of() : query));
    }

    /**
     * 获取环境。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/environment/{labId:\\d+}")
    public Result getEnvironment(@PathVariable Long labId) {
        return Result.success("获取实验室环境感知数据成功", iotDeviceService.getEnvironment(labId));
    }

    /**
     * 获取实验室Hardware状态。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/labs/{labId:\\d+}/status")
    public Result getLabHardwareStatus(@PathVariable Long labId) {
        return Result.success("获取实验室物联网状态成功", iotHardwareService.getLabStatus(labId));
    }

    /**
     * 执行Access命令。
     * @param labId 实验室标识参数。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/iot/labs/{labId:\\d+}/access")
    public Result executeAccessCommand(@PathVariable Long labId, @RequestBody(required = false) HardwareActionRequest request) {
        String action = request == null || request.getAction() == null ? "open" : request.getAction();
        Map<String, Object> payload = request == null || request.getPayload() == null ? Collections.emptyMap() : request.getPayload();
        return Result.success("执行门禁指令成功", iotHardwareService.executeAccessCommand(labId, action, payload));
    }

    /**
     * 获取实验室Camera。
     * @param labId 实验室标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/labs/{labId:\\d+}/camera")
    public Result getLabCamera(@PathVariable Long labId) {
        return Result.success("获取实验室摄像头成功", iotHardwareService.getLabCamera(labId));
    }

    /**
     * 获取设备状态。
     * @param code 编码参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/iot/devices/{code}/status")
    public Result getDeviceStatus(@PathVariable String code) {
        return Result.success("获取物联网设备状态成功", iotHardwareService.getDeviceStatus(code));
    }

    /**
     * 执行设备命令。
     * @param code 编码参数。
     * @param action action参数。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/iot/devices/{code}/commands/{action}")
    public Result executeDeviceCommand(
            @PathVariable String code,
            @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        return Result.success("执行物联网设备指令成功", iotHardwareService.executeDeviceCommand(
                code,
                action,
                payload == null ? Collections.emptyMap() : payload
        ));
    }

    /**
     * 获取Camera快照。
     * @param code 编码参数。
     * @return ResponseEntity<byte[]> 类型的处理结果。
     */
    @GetMapping("/iot/cameras/{code}/snapshot")
    public ResponseEntity<byte[]> getCameraSnapshot(@PathVariable String code) {
        IotHardwareService.CameraSnapshot snapshot = iotHardwareService.getCameraSnapshot(code);
        MediaType mediaType = org.example.backend.security.SafeRasterMediaType.parse(
                snapshot.contentType() == null ? MediaType.IMAGE_JPEG_VALUE : snapshot.contentType());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header("Content-Disposition", MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)
                        ? "attachment; filename=\"snapshot.bin\"" : "inline")
                .body(snapshot.bytes());
    }

    public static class HardwareActionRequest {
        private String action;
        private Map<String, Object> payload;

        /**
         * 获取Action。
         * @return String 类型的处理结果。
         */
        public String getAction() {
            return action;
        }

        /**
         * 设置Action。
         * @param action action参数。
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * 获取载荷。
         * @return 当前操作返回的键值结果。
         */
        public Map<String, Object> getPayload() {
            return payload;
        }

        /**
         * 设置载荷。
         * @param payload 载荷参数。
         */
        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }
    }
}
