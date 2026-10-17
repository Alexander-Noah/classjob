/*
 * 耗材借还的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/consumables/loans、/{id}、/{id}/return；完整路径由类级与方法级映射组合。
 * 主要协作组件：ConsumableLoanService。
 */
package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.service.ConsumableLoanService;
import org.example.backend.security.JwtAuthenticationFilter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumables/loans")
public class ConsumableLoanController {
    private final ConsumableLoanService service;
    /**
     * 构造 ConsumableLoanController，初始化当前实例。
     * @param service 服务参数。
     */
    public ConsumableLoanController(ConsumableLoanService service) { this.service = service; }
    /**
     * 列出当前操作。
     * @return Result 类型的处理结果。
     */
    @GetMapping
    public Result list() { return Result.success("获取借用记录成功", service.list()); }
    /**
     * 获取当前操作。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/{id}")
    public Result get(@PathVariable Long id) { return Result.success("获取借用详情成功", service.get(id)); }
    /**
     * 借出当前操作。
     * @param request 请求参数。
     * @param username username参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping
    public Result borrow(@RequestBody ConsumableLoanService.BorrowRequest request,
                         @RequestAttribute(JwtAuthenticationFilter.AUTH_USERNAME_ATTRIBUTE) String username) {
        return Result.success("借用登记成功", service.borrow(request, username));
    }
    /**
     * 归还条目集合。
     * @param id 标识参数。
     * @param request 请求参数。
     * @param username username参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/{id}/return")
    public Result returnItems(@PathVariable Long id, @RequestBody ConsumableLoanService.ReturnRequest request,
                              @RequestAttribute(JwtAuthenticationFilter.AUTH_USERNAME_ATTRIBUTE) String username) {
        return Result.success("归还登记成功", service.returnItems(id, request, username));
    }
}
