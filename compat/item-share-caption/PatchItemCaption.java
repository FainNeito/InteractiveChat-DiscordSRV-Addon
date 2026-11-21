import java.nio.file.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;

public final class PatchItemCaption {
    public static void main(String[] args) throws Exception {
        String expected = "d533dd11109bc5d842e7a6ca8e8b62d61947b9463e18b4c76e5f6fc62544cf4a";
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            try (var stream = new java.security.DigestInputStream(input, digest)) {
                stream.transferTo(java.io.OutputStream.nullOutputStream());
            }
            if (!java.util.HexFormat.of().formatHex(digest.digest()).equals(expected))
                throw new IllegalArgumentException("Unexpected base addon JAR");
        }
        String entry = "com/loohp/interactivechatdiscordsrvaddon/listeners/OutboundToDiscordEvents.class";
        byte[] bytes;
        try (JarFile jar = new JarFile(args[0])) {
            bytes = jar.getInputStream(jar.getJarEntry(entry)).readAllBytes();
        }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        String component = "Lcom/loohp/interactivechat/libs/net/kyori/adventure/text/Component;";
        int[] matches = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                var visitor = super.visitMethod(a,n,d,s,e);
                if (!n.equals("processGameMessage")) return visitor;
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    public void visitMethodInsn(int op, String owner, String method, String desc, boolean iface) {
                        super.visitMethodInsn(op,owner,method,desc,iface);
                        if (owner.equals("com/loohp/interactivechat/utils/ItemStackUtils") && method.equals("getDisplayName")
                                && desc.equals("(Lorg/bukkit/inventory/ItemStack;)" + component)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/loohp/interactivechatdiscordsrvaddon/utils/DiscordItemNamePresentation",
                                    "format", "(" + component + ")" + component, false);
                            matches[0]++;
                        }
                    }
                };
            }
        }, 0);
        if (matches[0] != 1) throw new IllegalStateException("Expected exactly one caption call, found " + matches[0]);
        Path out = Path.of(args[1],entry);
        Files.createDirectories(out.getParent());
        Files.write(out,writer.toByteArray());
    }
}
