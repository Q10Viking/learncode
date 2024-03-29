package org.hzz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hzz.domain.common.Result;
import org.hzz.domain.dto.EmailQueryDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Tag(name = "测试")
@RequestMapping("/test")
public interface TestApi {

    @Operation(summary = "测试异常接口")
    @GetMapping("/exception")
    default Result<String> exception() {
        throw new RuntimeException("测试异常");
    }

    @Operation(summary = "测试hello接口")
    @GetMapping("/hello")
    default Result<String> hello() {
        return Result.success("hello Q10Viking");
    }

    @Operation(summary = "测试校验getter还是setter")
    @GetMapping("/validate")
    default Result<String> validate(@Valid EmailQueryDTO phoneQueryDTO) {
        System.out.println("....");
        return Result.success(phoneQueryDTO.getEmail());
    }
}
