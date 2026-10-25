// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import hu.elte.ik.thesis.cinegrade.domain.enums.BlobKind;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;

/**
 * One encoded image (thumbnail or preview) of a photo, as stored in the preview database.
 */
@Value
@Builder(toBuilder = true)
public class PhotoBlob {
    @NonNull
    String photoId;
    @NonNull
    BlobKind kind;
    @NonNull
    String mime;
    Integer width;
    Integer height;
    @NonNull
    @ToString.Exclude // can be hundreds of KB, keep it out of logs
    byte[] data;
    Instant createdAt;
}
