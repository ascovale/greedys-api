import java.util.Map;
import com.application.common.spring.swagger.vendor.*;
import com.application.common.spring.swagger.vendor.builder.VendorExtensionBuilder;

public class EsempiWrapperExtensions {
    
    public static void exempleUsage() {
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
            
            @Override
            public int getPriority() { return 100; }
        };

        // Registrazione nel registry
        VendorExtensionRegistry registry = new VendorExtensionRegistry();
        registry.register(wrapperOnlyExtension);

        // OPPURE usando il builder (più semplice):
        VendorExtension sameExtensionWithBuilder = VendorExtensionBuilder.create("x-wrapper-metadata")
                .mapValue()
                .addToMap("isResponseWrapper", true)
                .addToMap("hasTimestamp", true) 
                .addToMap("hasMessage", true)
                .onlyWrappers()  // <-- Stesso effetto del shouldApply personalizzato
                .build();
                
        registry.register(sameExtensionWithBuilder);
        
        System.out.println("Extensions registrate: " + registry.getStats());
    }
}
