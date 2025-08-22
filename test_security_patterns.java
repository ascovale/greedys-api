import com.application.common.security.SecurityPatterns;

/**
 * Test rapido per verificare i SecurityPatterns unificati
 */
public class TestSecurityPatterns {
    public static void main(String[] args) {
        System.out.println("=== TESTING UNIFIED SECURITY PATTERNS ===\n");
        
        // Test pattern pubblici
        testPublicPaths();
        
        // Test pattern refresh token
        testRefreshTokenPaths();
        
        // Test pattern Hub
        testHubPaths();
        
        // Stampa tutti i pattern
        System.out.println("\n=== ALL PATTERNS ===");
        SecurityPatterns.printAllPatterns();
    }
    
    private static void testPublicPaths() {
        System.out.println("🔓 TESTING PUBLIC PATHS:");
        String[] testPaths = {
            "/", "/index.html", "/favicon.ico", "/swagger-ui/index.html",
            "/restaurant/auth/login", "/customer/auth/login", "/admin/auth/login",
            "/v3/api-docs/swagger-config", "/actuator/health"
        };
        
        for (String path : testPaths) {
            boolean isPublic = SecurityPatterns.isPublicPath(path);
            System.out.printf("  %-35s -> %s\n", path, isPublic ? "✅ PUBLIC" : "❌ PROTECTED");
        }
    }
    
    private static void testRefreshTokenPaths() {
        System.out.println("\n🔄 TESTING REFRESH TOKEN PATHS:");
        String[] testPaths = {
            "/customer/auth/refresh", "/admin/auth/refresh", 
            "/restaurant/user/auth/refresh", "/restaurant/user/auth/refresh/hub",
            "/customer/orders", "/restaurant/menu"
        };
        
        for (String path : testPaths) {
            boolean isRefresh = SecurityPatterns.isRefreshTokenPath(path);
            System.out.printf("  %-35s -> %s\n", path, isRefresh ? "🔄 REFRESH REQUIRED" : "🎯 ACCESS TOKEN");
        }
    }
    
    private static void testHubPaths() {
        System.out.println("\n🏢 TESTING HUB ALLOWED PATHS:");
        String[] testPaths = {
            "/restaurant/switch-restaurant", "/restaurant/available-restaurants",
            "/restaurant/user/auth/restaurants", "/restaurant/auth/refresh",
            "/restaurant/orders", "/restaurant/menu", "/restaurant/profile/hub"
        };
        
        for (String path : testPaths) {
            boolean isHubAllowed = SecurityPatterns.isHubAllowedPath(path);
            System.out.printf("  %-35s -> %s\n", path, isHubAllowed ? "🏢 HUB ALLOWED" : "❌ HUB FORBIDDEN");
        }
    }
}
