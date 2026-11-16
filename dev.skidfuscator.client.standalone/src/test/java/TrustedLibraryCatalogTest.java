import dev.skidfuscator.obfuscator.command.TrustedLibraryCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TrustedLibraryCatalogTest {
 @Test void reviewedArtifactHasPinnedDigest() throws Exception {
  assertTrue(TrustedLibraryCatalog.checksum("org.apache.commons","commons-compress","1.28.0").matches("[0-9a-f]{64}"));
 }
 @Test void unknownArtifactIsNotAutomaticallyTrusted() {
  assertThrows(java.io.IOException.class,()->TrustedLibraryCatalog.checksum("example.untrusted","artifact","1.0"));
 }
}
