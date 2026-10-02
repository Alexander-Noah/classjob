package org.example.backend.labresource.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.platform.security.AuthenticationAttributes;
import org.example.backend.result.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@CrossOrigin
@RestController
public final class LabQueryController {
    private final LabCatalogQuery query;

    public LabQueryController(LabCatalogQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    @GetMapping("/labs")
    public Result getLabs(HttpServletRequest request) {
        return Result.success("查询实验室列表成功", query.list(resolveScope(request)));
    }

    @GetMapping("/labs/{id:\\d+}")
    public Result getLabById(@PathVariable long id) {
        return Result.success("查询实验室详情成功", query.findById(id));
    }

    private LabAccessScope resolveScope(HttpServletRequest request) {
        Object role = request.getAttribute(AuthenticationAttributes.ROLE);
        if ("systemAdmin".equals(String.valueOf(role))) {
            return LabAccessScope.all();
        }

        Object rawUserId = request.getAttribute(AuthenticationAttributes.USER_ID);
        if (rawUserId == null) {
            return LabAccessScope.all();
        }
        try {
            return LabAccessScope.managedBy(Long.parseLong(String.valueOf(rawUserId)));
        } catch (NumberFormatException exception) {
            return LabAccessScope.all();
        }
    }
}
