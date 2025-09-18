package com.application.common.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/swagger-groups")
public class SwaggerGroupsController {

    private final List<GroupedOpenApi> groupedOpenApis;

    public SwaggerGroupsController(List<GroupedOpenApi> groupedOpenApis) {
        this.groupedOpenApis = groupedOpenApis;
    }

    @GetMapping
    public List<String> getGroups() {
        return groupedOpenApis.stream()
                .map(GroupedOpenApi::getGroup)
                .collect(Collectors.toList());
    }
}
