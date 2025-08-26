package com.application.common.spring.swagger;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable data class that holds complete information about a wrapper type discovered from API operations.
 * This represents a unique combination of data type + wrapper type.
 * 
 * Examples:
 * - ResponseWrapper<UserDto> -> dataClassName="com.app.UserDto", wrapperType="DTO"
 * - ResponseWrapper<List<UserDto>> -> dataClassName="com.app.UserDto", wrapperType="LIST"  
 * - ResponseWrapper<Page<UserDto>> -> dataClassName="com.app.UserDto", wrapperType="PAGE"
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class WrapperTypeInfo {
    @EqualsAndHashCode.Include
    public final String dataClassName;    // Full class name (e.g., "com.application.admin.web.dto.UserDto")
    @EqualsAndHashCode.Include
    public final String wrapperType;      // Type of wrapper: "DTO", "LIST", or "PAGE"
    public final String description;      // Human-readable description for API docs
    
    /**
     * Extract simple class name from full package path
     * "com.application.admin.web.dto.UserDto" -> "UserDto"
     */
    public String getDataTypeSimpleName() {
        return dataClassName.substring(dataClassName.lastIndexOf('.') + 1);
    }
    
    /**
     * Generate the OpenAPI schema name for the wrapper type
     * Examples:
     * - DTO: "ResponseWrapperUserDto"
     * - LIST: "ResponseWrapperListUserDto" 
     * - PAGE: "ResponseWrapperPageUserDto"
     */
    public String getWrapperSchemaName() {
        String dataTypeSimpleName = getDataTypeSimpleName();
        switch (wrapperType) {
            case "DTO":
                return "ResponseWrapper" + dataTypeSimpleName;
            case "LIST":
                return "ResponseWrapperList" + dataTypeSimpleName;
            case "PAGE":
                return "ResponseWrapperPage" + dataTypeSimpleName;
            default:
                return "ResponseWrapper" + dataTypeSimpleName;
        }
    }
    
    /**
     * Generate schema name for List<T> array schema
     * Example: "ListUserDto" for List<UserDto>
     */
    public String getListSchemaName() {
        return "List" + getDataTypeSimpleName();
    }
    
    /**
     * Generate schema name for Page<T> object schema
     * Example: "PageUserDto" for Page<UserDto>
     */
    public String getPageSchemaName() {
        return "Page" + getDataTypeSimpleName();
    }
    
    /**
     * Generate the complete OpenAPI schema reference for the wrapper type
     * Examples:
     * - DTO: "#/components/schemas/ResponseWrapperUserDto"
     * - LIST: "#/components/schemas/ResponseWrapperListUserDto" 
     * - PAGE: "#/components/schemas/ResponseWrapperPageUserDto"
     */
    public String getWrapperSchemaReference() {
        return "#/components/schemas/" + getWrapperSchemaName();
    }
}
