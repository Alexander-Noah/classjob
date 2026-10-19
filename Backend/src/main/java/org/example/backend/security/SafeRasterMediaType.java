package org.example.backend.security;

import org.springframework.http.MediaType;
import java.util.Locale;
import java.util.Set;

/** Never serve upstream HTML/SVG/script as an executable same-origin document. */
public final class SafeRasterMediaType {
    private SafeRasterMediaType() {}
    public static MediaType parse(String value) {
        try {
            MediaType type = MediaType.parseMediaType(value);
            String mime = (type.getType() + "/" + type.getSubtype()).toLowerCase(Locale.ROOT);
            return Set.of("image/png", "image/jpeg", "image/gif", "image/webp").contains(mime)
                    ? MediaType.parseMediaType(mime) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) { return MediaType.APPLICATION_OCTET_STREAM; }
    }
}
