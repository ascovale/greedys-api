# OpenAPI ResponseWrapper<T> Issues - Fixes To Do

## 🚨 Critical Issues Found

### 1. Missing Schema Definitions (36 broken references)

**Problem**: API endpoints reference 36 ResponseWrapper schemas that don't exist in the components section.

**Impact**: Dart code generation will fail with "Schema not found" errors.

**Status**: ❌ **CRITICAL - BLOCKS CODE GENERATION**

---

## 📋 Missing Schemas to Define

### Single Object Wrappers (25 missing)
- [ ] `ResponseWrapperAuthResponseDTO`
- [ ] `ResponseWrapperBoolean`
- [ ] `ResponseWrapperCustomerStatisticsDTO`
- [ ] `ResponseWrapperDishDTO`
- [ ] `ResponseWrapperLong`
- [ ] `ResponseWrapperMenuDTO`
- [ ] `ResponseWrapperMenuDishDTO`
- [ ] `ResponseWrapperRUserDTO`
- [ ] `ResponseWrapperRUserFcmToken`
- [ ] `ResponseWrapperReservation`
- [ ] `ResponseWrapperReservationDTO`
- [ ] `ResponseWrapperRestaurantAuthorizationResult`
- [ ] `ResponseWrapperRestaurantDTO`
- [ ] `ResponseWrapperRestaurantNotificationDTO`
- [ ] `ResponseWrapperRestaurantSearchResult`
- [ ] `ResponseWrapperRoomDTO`
- [ ] `ResponseWrapperServiceDTO`
- [ ] `ResponseWrapperSlotDTO`
- [ ] `ResponseWrapperString`
- [ ] `ResponseWrapperTableDTO`

### List Wrappers (12 missing)
- [ ] `ResponseWrapperListDishDTO`
- [ ] `ResponseWrapperListLocalDate`
- [ ] `ResponseWrapperListMenuDTO`
- [ ] `ResponseWrapperListMenuDishDTO`
- [ ] `ResponseWrapperListReservationDTO`
- [ ] `ResponseWrapperListRestaurantDTO`
- [ ] `ResponseWrapperListRestaurantNotificationDTO`
- [ ] `ResponseWrapperListRoomDTO`
- [ ] `ResponseWrapperListServiceDTO`
- [ ] `ResponseWrapperListServiceTypeDto`
- [ ] `ResponseWrapperListSlotDTO`
- [ ] `ResponseWrapperListString`
- [ ] `ResponseWrapperListTableDTO`

### Page Wrappers (2 missing)
- [ ] `ResponseWrapperPageReservationDTO`
- [ ] `ResponseWrapperPageRestaurantNotificationDTO`

---

## 🔧 Immediate Fixes Required

### Fix 1: Add Missing Schema Definitions
**Priority**: 🔴 **CRITICAL**

Add all 36 missing schemas to `components.schemas` section using `allOf` composition:

```json
"ResponseWrapperReservationDTO": {
  "allOf": [
    {"$ref": "#/components/schemas/ResponseWrapperSingle"},
    {
      "properties": {
        "data": {"$ref": "#/components/schemas/ReservationDTO"}
      }
    }
  ]
}
```

**Estimated Work**: 2-3 hours manual work OR automated script

### Fix 2: Fix Hardcoded Type in Base ResponseWrapper
**Priority**: 🟡 **MEDIUM**

Current base `ResponseWrapper` schema hardcodes `ReservationDTO`:
```json
"data": {"$ref": "#/components/schemas/ReservationDTO"}  // ← Remove this hardcoding
```

**Should be**: Generic object or remove this base schema entirely.

### Fix 3: Improve Base Schema Structure
**Priority**: 🟡 **MEDIUM**

Current base schemas use generic `"type": "object"` for data fields. This works but isn't optimal for type safety.

**Current**:
```json
"data": {"type": "object", "description": "Response data (type varies)"}
```

**Better**: Specific type references in each concrete wrapper.

---

## 🛠️ Implementation Options

### Option A: Manual Schema Addition (RECOMMENDED)
**Effort**: Medium | **Time**: 2-3 hours | **Risk**: Low

1. Create script to generate all 36 missing schemas
2. Add them to swagger.json components section
3. Test with Dart code generation tool

### Option B: SpringDoc Configuration Changes
**Effort**: High | **Time**: 1-2 days | **Risk**: Medium

1. Modify SpringDoc configuration
2. Add custom schema generation logic
3. Regenerate entire OpenAPI spec
4. Test all endpoints

### Option C: Post-Processing Script
**Effort**: Medium | **Time**: 4-6 hours | **Risk**: Low

1. Create automated script to fix swagger.json after generation
2. Run as part of build process
3. Maintain list of required wrapper types

---

## 📝 Specific Schema Templates

### Single Object Wrapper Template
```json
"ResponseWrapper[TypeName]": {
  "allOf": [
    {"$ref": "#/components/schemas/ResponseWrapperSingle"},
    {
      "properties": {
        "data": {"$ref": "#/components/schemas/[TypeName]"}
      }
    }
  ]
}
```

### List Wrapper Template
```json
"ResponseWrapperList[TypeName]": {
  "allOf": [
    {"$ref": "#/components/schemas/ResponseWrapperList"},
    {
      "properties": {
        "data": {
          "type": "array",
          "items": {"$ref": "#/components/schemas/[TypeName]"}
        }
      }
    }
  ]
}
```

### Page Wrapper Template
```json
"ResponseWrapperPage[TypeName]": {
  "allOf": [
    {"$ref": "#/components/schemas/ResponseWrapperPage"},
    {
      "properties": {
        "data": {
          "type": "object",
          "properties": {
            "content": {
              "type": "array",
              "items": {"$ref": "#/components/schemas/[TypeName]"}
            }
          }
        }
      }
    }
  ]
}
```

---

## 🧪 Testing Checklist

After implementing fixes:

- [ ] Validate OpenAPI spec with online validator
- [ ] Test Dart code generation with `json_serializable`
- [ ] Test Dart code generation with `freezed`
- [ ] Verify all 36 ResponseWrapper types generate proper Dart classes
- [ ] Test actual API calls with generated Dart models
- [ ] Check for any remaining "Object" types in generated code

---

## 📊 Current Status Summary

| Issue Category | Count | Status | Impact |
|---------------|--------|--------|---------|
| Missing Schemas | 36 | ❌ Critical | Blocks code generation |
| Generic Type Issues | 4 | 🟡 Medium | Reduces type safety |
| Hardcoded Types | 1 | 🟡 Medium | Incorrect schema |
| **TOTAL ISSUES** | **41** | **❌ CRITICAL** | **CODE GEN FAILURE** |

---

## ⏰ Recommended Timeline

1. **Day 1**: Implement Option A (manual schema addition)
2. **Day 2**: Test with Dart code generation
3. **Day 3**: Fix any remaining issues
4. **Week 2**: Implement long-term solution (Option B or C)

---

## 🎯 Success Criteria

✅ **Fixed when**:
- All 36 missing schemas are defined
- Dart code generation completes without errors
- Generated Dart models have proper type safety (no `Object` types)
- API responses can be parsed correctly in Flutter app

---

## 💡 Prevention for Future

1. Add automated tests for OpenAPI schema completeness
2. Implement CI/CD check for broken schema references
3. Set up automated Dart code generation in build pipeline
4. Document SpringDoc configuration for generic types