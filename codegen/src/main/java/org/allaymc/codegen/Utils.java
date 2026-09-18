package org.allaymc.codegen;

import com.google.gson.JsonParser;
import org.allaymc.dependence.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author daoge_cmd | IWareQ
 */
public class Utils {

    /** Bir paketten bu kadar import varsa yıldızla yazılır (IntelliJ varsayılanıyla aynı). */
    private static final int WILDCARD_IMPORT_THRESHOLD = 5;

    public static String convertToPascalCase(String str) {
        var parts = StringUtils.fastSplit(str, "_");
        var output = new StringBuilder();

        for (var part : parts) {
            output.append(Character.toUpperCase(part.charAt(0)));
            output.append(part.substring(1));
        }

        return output.toString();
    }

    public static String camelCaseToSnakeCase(String str) {
        var output = new StringBuilder();

        var chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            var ch = chars[i];
            if (Character.isUpperCase(ch)) {
                if (i > 0) output.append('_');
                output.append(Character.toLowerCase(ch));
            } else {
                output.append(ch);
            }
        }

        return output.toString();
    }

    public static Set<String> parseKeys(Path path) {
        Set<String> keys = new HashSet<>();
        try (var reader = new InputStreamReader(Files.newInputStream(path))) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> keys.add(entry.getKey()));
            return keys;
        } catch (IOException e) {
            throw new CodeGenException(e);
        }
    }

    public static void writeFileWithCRLF(Path path, String content) throws IOException {
        String crlfContent = collapseImports(content).replace("\n", "\r\n");
        Files.writeString(path, crlfContent);
    }

    /**
     * GearsMC: JavaPoet her türü ayrı ayrı import eder. Tür listesi üreten dosyalar (ItemTypes, BlockTypes,
     * varsayılan başlatıcılar) böylece yüzlerce import satırıyla başlıyor; depoda o dosyalar yıldızlı duruyordu çünkü
     * üretimden sonra IDE import'ları topluyordu. Üretici IDE'siz çalıştırılınca fark commit'e giriyor, bu yüzden
     * toplama burada yapılır: aynı paketten en az {@link #WILDCARD_IMPORT_THRESHOLD} import varsa yıldıza indirilir.
     *
     * <p>Statik import'lara ve tek tük import'lara dokunulmaz. Sıralama JavaPoet'teki gibi alfabetiktir.</p>
     */
    static String collapseImports(String content) {
        var lines = content.split("\n", -1);
        var firstImport = -1;
        var lastImport = -1;
        var counts = new TreeMap<String, Integer>();
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (!line.startsWith("import ") || !line.endsWith(";")) {
                continue;
            }
            if (firstImport == -1) {
                firstImport = i;
            }
            lastImport = i;
            var packageName = packageOf(line);
            if (packageName != null) {
                counts.merge(packageName, 1, Integer::sum);
            }
        }
        if (firstImport == -1) {
            return content;
        }

        var collapsed = new TreeSet<String>();
        counts.forEach((packageName, count) -> {
            if (count >= WILDCARD_IMPORT_THRESHOLD) {
                collapsed.add(packageName);
            }
        });
        if (collapsed.isEmpty()) {
            return content;
        }

        var imports = new TreeSet<String>();
        for (int i = firstImport; i <= lastImport; i++) {
            var line = lines[i];
            if (!line.startsWith("import ") || !line.endsWith(";")) {
                continue;
            }
            var packageName = packageOf(line);
            imports.add(packageName != null && collapsed.contains(packageName) ? "import " + packageName + ".*;" : line);
        }

        var result = new StringBuilder();
        for (int i = 0; i < firstImport; i++) {
            result.append(lines[i]).append('\n');
        }
        imports.forEach(line -> result.append(line).append('\n'));
        for (int i = lastImport + 1; i < lines.length; i++) {
            result.append(lines[i]);
            if (i < lines.length - 1) {
                result.append('\n');
            }
        }
        return result.toString();
    }

    /** Statik olmayan import'un paket adı; statik import ve biçimsiz satırda {@code null}. */
    private static String packageOf(String importLine) {
        if (importLine.startsWith("import static ")) {
            return null;
        }
        var typeName = importLine.substring("import ".length(), importLine.length() - 1).trim();
        var lastDot = typeName.lastIndexOf('.');
        return lastDot < 0 ? null : typeName.substring(0, lastDot);
    }
}
