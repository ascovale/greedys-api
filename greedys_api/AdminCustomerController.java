// ESEMPIO DI CONTROLLER AGGIORNATO

@RestController
@RequestMapping("/admin/customers")
@Tag(name = "Admin Customer Management", description = "Admin endpoints for managing customers")
public class AdminCustomerController extends BaseController {

    @Autowired
    private AdminCustomerService adminCustomerService;

    // ===================== ESEMPIO SINGLE OBJECT ======================
    
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "Get customer by ID", description = "Returns a specific customer by ID")
    @GetMapping("/{id}")
    @WrapperType(dataClass = CustomerDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<CustomerDTO>> getCustomer(@PathVariable Long id) {
        return execute("get customer", () -> adminCustomerService.findById(id));
    }

    // ===================== ESEMPIO LIST ======================
    
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List all customers", description = "Returns all customers")
    @GetMapping("/all")
    @WrapperType(dataClass = CustomerDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<CustomerDTO>>> listAllCustomers() {
        return executeList("list all customers", () -> adminCustomerService.findAll());
    }

    // ===================== ESEMPIO PAGE ======================
    
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List customers with pagination", description = "Returns a paginated list of customers")
    @GetMapping("/page")
    @WrapperType(dataClass = CustomerDTO.class, type = WrapperDataType.PAGE)
    public ResponseEntity<ResponseWrapper<Page<CustomerDTO>>> listCustomersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return executePaginatedAsPage("list customers", 
            () -> adminCustomerService.findAll(PageRequest.of(page, size)));
    }

    // ===================== ESEMPIO CREATE ======================
    
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_CREATE')")
    @Operation(summary = "Create new customer", description = "Creates a new customer")
    @PostMapping
    @WrapperType(dataClass = CustomerDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<CustomerDTO>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return executeCreate("create customer", "Customer created successfully", 
            () -> adminCustomerService.create(request));
    }

    // ===================== ESEMPIO DELETE (VOID) ======================
    
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_DELETE')")
    @Operation(summary = "Delete customer", description = "Deletes a customer by ID")
    @DeleteMapping("/{id}")
    @WrapperType(dataClass = String.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<String>> deleteCustomer(@PathVariable Long id) {
        return executeVoid("delete customer", "Customer deleted successfully", 
            () -> adminCustomerService.deleteById(id));
    }
}
