# WrapperType Annotation - Usage Examples

L'annotazione `@WrapperType` permette di specificare automaticamente il tipo di schema OpenAPI basato sul wrapper utilizzato nei controller.

## Sintassi

```java
@WrapperType(dataClass = <DTO_CLASS>.class, type = WrapperDataType.<TYPE>)
```

## Tipi Supportati

### 1. DTO - Oggetto Singolo
Per restituire un singolo oggetto avvolto in `ResponseWrapper<T>`:

```java
@GetMapping("/user/{id}")
@WrapperType(dataClass = UserDTO.class, type = WrapperDataType.DTO)
public ResponseWrapper<UserDTO> getUserById(@PathVariable Long id) {
    return execute("get user", () -> userService.findById(id));
}
```

**Schema generato**: `ResponseWrapper<UserDTO>`
```json
{
  "success": true,
  "message": "Success",
  "data": {
    // UserDTO fields
  },
  "errors": []
}
```

### 2. LIST - Lista di Oggetti  
Per restituire una lista di oggetti avvolti in `ListResponseWrapper<T>`:

```java
@GetMapping("/users")
@WrapperType(dataClass = UserDTO.class, type = WrapperDataType.LIST)
public ListResponseWrapper<UserDTO> getAllUsers() {
    return executeList("get users", () -> userService.findAll());
}
```

**Schema generato**: `ListResponseWrapper<UserDTO>`
```json
{
  "success": true,
  "message": "Success", 
  "data": [
    {
      // UserDTO fields
    }
  ],
  "errors": []
}
```

### 3. PAGE - Dati Paginati
Per restituire dati paginati avvolti in `PageResponseWrapper<T>`:

```java
@GetMapping("/users/page")
@WrapperType(dataClass = UserDTO.class, type = WrapperDataType.PAGE)
public PageResponseWrapper<UserDTO> getUsersWithPagination(@RequestParam int page, @RequestParam int size) {
    return executePaginated("get users paginated", () -> userService.findAll(PageRequest.of(page, size)));
}
```

**Schema generato**: `PageResponseWrapper<UserDTO>`
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        // UserDTO fields
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false,
    "numberOfElements": 10,
    "empty": false
  },
  "errors": []
}
```

## Vantaggi

1. **Documentazione Automatica**: Lo schema OpenAPI viene generato automaticamente
2. **Type Safety**: Il tipo di dato è specificato esplicitamente
3. **Consistenza**: Garantisce strutture di risposta coerenti
4. **Maintenance**: Facile manutenzione e aggiornamento degli schemi

## Note di Implementazione

- L'annotazione deve essere usata sui metodi dei controller
- Il `dataClass` deve essere la classe DTO che verrà serializzata
- Il `type` determina la struttura del wrapper nella documentazione OpenAPI
- Gli schemi vengono generati automaticamente dal `WrapperTypeCustomizer`
