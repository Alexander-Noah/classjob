/*
 * 实验室预约的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/public/labs/{id:\\d+}/reservation-profile、/public/reservations/scan、/public/reservations/{id:\\d+}/status、/reservations/{id:\\d+}、/reservations/batch；完整路径由类级与方法级映射组合。
 * 主要协作组件：ReservationsService。
 */
package org.example.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.result.Result;
import org.example.backend.service.ReservationsService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@CrossOrigin
@RestController
@Slf4j
public class ReservationsController {
    private final ReservationsService reservationsService;

    /**
     * 构造 ReservationsController，初始化当前实例。
     * @param reservationsService 预约服务参数。
     */
    public ReservationsController(ReservationsService reservationsService) {
        this.reservationsService = reservationsService;
    }

    /**
     * 获取Scan预约Profile。
     * @param id 标识参数。
     * @param code 编码参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/public/labs/{id:\\d+}/reservation-profile")
    public Result getScanReservationProfile(
            @PathVariable("id") Integer id,
            @RequestParam(value = "code", required = false) String code
    ) {
        return Result.success("获取扫码预约资料成功", reservationsService.getScanReservationProfile(id, code));
    }

    /**
     * 提交Scan预约。
     * @param reservationsEntity 预约Entity参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/public/reservations/scan")
    public Result submitScanReservation(@RequestBody ReservationsEntity reservationsEntity) {
        return Result.success("提交扫码预约成功", reservationsService.submitScanReservation(reservationsEntity));
    }

    /**
     * 获取Scan预约状态。
     * @param id 标识参数。
     * @param contact contact参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/public/reservations/{id:\\d+}/status")
    public Result getScanReservationStatus(
            @PathVariable("id") Integer id,
            @RequestParam(value = "contact", required = false) String contact
    ) {
        return Result.success("获取扫码预约状态成功", reservationsService.getScanReservationStatus(id, contact));
    }

    /**
     * 更新预约。
     * @param id 标识参数。
     * @param reservationsEntity 预约Entity参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/reservations/{id:\\d+}")
    public Result updateReservation(@PathVariable("id") Integer id, @RequestBody ReservationsEntity reservationsEntity) {
        reservationsService.updateReservation(id, reservationsEntity);
        return Result.success("更新预约成功", reservationsService.getReservation(id));
    }

    /**
     * replace预约的处理逻辑。
     * @param request 请求参数。
     * @return Result 类型的处理结果。
     */
    @PutMapping("/reservations/batch")
    public Result replaceReservations(@RequestBody ReservationBatchRequest request) {
        List<ReservationsEntity> reservations = request == null || request.getRecords() == null
                ? Collections.emptyList()
                : request.getRecords();
        return Result.success("批量保存预约成功", reservationsService.replaceReservations(reservations));
    }

    /**
     * 删除预约。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/reservations/{id:\\d+}")
    public Result deleteReservation(@PathVariable("id") Integer id) {
        reservationsService.deleteReservation(id);
        return Result.success("删除预约成功");
    }

    /**
     * 重置预约。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/reservations/reset")
    public Result resetReservations() {
        return Result.success("重置预约数据成功", reservationsService.getReservations());
    }

    public static class ReservationBatchRequest {
        private String resource;
        private List<ReservationsEntity> records;

        /**
         * 获取资源。
         * @return String 类型的处理结果。
         */
        public String getResource() {
            return resource;
        }

        /**
         * 设置资源。
         * @param resource 资源参数。
         */
        public void setResource(String resource) {
            this.resource = resource;
        }

        /**
         * 获取记录集合。
         * @return 当前操作得到的记录集合。
         */
        public List<ReservationsEntity> getRecords() {
            return records;
        }

        /**
         * 设置记录集合。
         * @param records 记录集合参数。
         */
        public void setRecords(List<ReservationsEntity> records) {
            this.records = records;
        }
    }
}
