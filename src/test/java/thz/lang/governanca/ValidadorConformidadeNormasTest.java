package thz.lang.governanca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.*;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.Monetario;
import thz.lang.security.ThzSecurity;
import thz.lang.version.ThzVersion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de Verificação e Validação 1 a 1 de Conformidade com Normas Internacionais (ISO, JSR, RFC).
 */
public class ValidadorConformidadeNormasTest {

    @Test
    @DisplayName("Conformidade ISO/IEC 10967: Aritmética decimal exata proíbe erros de ponto flutuante")
    void validarIso10967_AritmeticaDecimalExata() {
        DecimalFixo itemA = DecimalFixo.deTexto("0.10");
        DecimalFixo itemB = DecimalFixo.deTexto("0.20");
        DecimalFixo soma = itemA.somar(itemB);

        assertEquals("0.3000", soma.toString());
        assertNotEquals(0.30000000000000004, Double.parseDouble(soma.toString()));
    }

    @Test
    @DisplayName("Conformidade ISO 4217: Validação de moedas alfa-3 e rejeição de soma entre moedas distintas")
    void validarIso4217_CodigosMoedas() {
        Monetario brl = Monetario.deTexto("100.00", "BRL");
        Monetario usd = Monetario.deTexto("50.00", "USD");

        assertEquals("BRL", brl.moeda.codigo());
        assertEquals("USD", usd.moeda.codigo());
        assertThrows(RuntimeException.class, () -> brl.somar(usd));
    }

    @Test
    @DisplayName("Conformidade ISO/IEC/IEEE 42010: Preservação de metadados de arquitetura na AST")
    void validarIso42010_MetadadosArquitetura() {
        MetadadosArquiteturaAst metadados = new MetadadosArquiteturaAst(
                "Financeiro",
                "FaturamentoCore",
                "Dominio",
                "2.4.0",
                "Lucas Thomaz",
                "50ms",
                List.of("SOX-404")
        );

        assertEquals("Financeiro", metadados.dominio());
        assertEquals("FaturamentoCore", metadados.subdominio());
        assertEquals("50ms", metadados.sloLatencia());
    }

    @Test
    @DisplayName("Conformidade RFC 4122: Geração e validação de UUID v4 de 128-bits")
    void validarRfc4122_UuidV4() {
        String uuid = ThzSecurity.gerarUuid();

        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"));
    }

    @Test
    @DisplayName("Conformidade SemVer 2.0.0: Parsing e comparação determinística de versões")
    void validarSemVer200_Versoes() {
        ThzVersion v1 = ThzVersion.parse("2.4.0");
        ThzVersion v2 = ThzVersion.parse("2.4.1");

        assertTrue(v1.compareTo(v2) < 0);
        assertEquals(2, v1.major());
        assertEquals(4, v1.minor());
        assertEquals(0, v1.patch());
    }
}
