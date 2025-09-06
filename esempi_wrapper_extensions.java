// Esempio: Extension che si applica solo ai wrapper
VendorExtension wrapperOnlyExtension = new VendorExtension() {
    @Override
    public String getName() { return "x-wrapper-metadata"; }
    
    @Override
    public Object getValue() {
        return Map.of(
            "isResponseWrapper", true,
            "hasTimestamp", true,
            "hasMessage", true
        );
    }
    
    @Override
    public VendorExtensionLevel getLevel() { return VendorExtensionLevel.SCHEMA; }
    
    @Override
    public String getTarget() { return "*"; } // Tutti gli schemi...
    
    @Override
    public boolean shouldApply(String target, Map<String, Object> context) {
        // ...ma solo se inizia con "ResponseWrapper"
        return target != null && target.startsWith("ResponseWrapper");
    }
};

// Registrazione
vendorExtensionRegistry.register(wrapperOnlyExtension);
