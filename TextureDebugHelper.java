// Debug script to check texture paths
public class TextureDebugHelper {
    public static void checkShadowAltarTextures() {
        System.out.println("=== SHADOW ALTAR TEXTURE DEBUG ===");
        
        // Check if texture files exist
        String[] texturePaths = {
            "src/main/resources/assets/theendupdate/textures/block/shadow_altar_top.png",
            "src/main/resources/assets/theendupdate/textures/block/shadow_altar_side.png"
        };
        
        for (String path : texturePaths) {
            java.io.File file = new java.io.File(path);
            System.out.println("Texture file: " + path + " exists: " + file.exists());
            if (file.exists()) {
                System.out.println("  - Size: " + file.length() + " bytes");
            }
        }
        
        // Check model files
        String[] modelPaths = {
            "src/main/resources/assets/theendupdate/models/block/shadow_altar.json",
            "src/main/resources/assets/theendupdate/models/item/shadow_altar.json",
            "src/main/resources/assets/theendupdate/blockstates/shadow_altar.json"
        };
        
        for (String path : modelPaths) {
            java.io.File file = new java.io.File(path);
            System.out.println("Model file: " + path + " exists: " + file.exists());
            if (file.exists()) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    System.out.println("  - Content: " + content);
                } catch (Exception e) {
                    System.out.println("  - Error reading: " + e.getMessage());
                }
            }
        }
        
        System.out.println("=== END TEXTURE DEBUG ===");
    }
}
