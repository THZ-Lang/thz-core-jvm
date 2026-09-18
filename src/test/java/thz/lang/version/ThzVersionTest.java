package thz.lang.version;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThzVersionTest {

    @Test
    @DisplayName("Deve fazer parsing e comparação SemVer 2.0.0")
    void testParsingSemver() {
        ThzVersion v1 = ThzVersion.parse("2.4.0");
        ThzVersion v2 = ThzVersion.parse("2.3.9");
        ThzVersion v3 = ThzVersion.parse("3.0.0-beta.1");

        assertTrue(v1.compareTo(v2) > 0);
        assertTrue(v1.compareTo(v3) < 0);
        assertEquals("2.4.0", v1.toString());
    }

    @Test
    @DisplayName("Deve verificar especificações de versão")
    void testSatisfaz() {
        assertTrue(ThzVersion.satisfaz("2.4.0", ">=2.0.0"));
        assertTrue(ThzVersion.satisfaz("2.4.0", "<=3.0.0"));
        assertTrue(ThzVersion.satisfaz("2.4.0", "^2.0.0"));
        assertFalse(ThzVersion.satisfaz("1.9.0", ">=2.0.0"));
    }

    @Test
    @DisplayName("Deve inspecionar informações de runtime")
    void testRuntimeInfo() {
        ThzVersion.RuntimeInfo info = ThzVersion.obterRuntimeInfo();
        assertNotNull(info);
        assertNotNull(info.versaoThz());
        assertTrue(info.cpuCores() > 0);
        assertTrue(info.memoriaMaxMb() > 0);
    }
}
