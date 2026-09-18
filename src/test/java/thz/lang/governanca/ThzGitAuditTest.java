package thz.lang.governanca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThzGitAuditTest {

    @Test
    @DisplayName("ThzGitAuditEngine deve auditar requisitos impactados e detectar regras sem contrato")
    void testAuditoriaGitEGovernanca() {
        RegraNegocioAst regraComRastreio = new RegraNegocioAst(
                "ProcessarDesconto",
                "RN-001",
                "REQ-FIN-100",
                "Aplica desconto em faturas",
                List.of(new ClausulaContratoAst("EXIGE", new ExprAst.LiteralLogico(true, 1, 1), "tamanho > 0", 1, 1)),
                List.of(),
                List.of()
        );

        RegraNegocioAst regraSemRastreio = new RegraNegocioAst(
                "CalculoBruto",
                null,
                null,
                "Sem rastreio",
                List.of(),
                List.of(),
                List.of()
        );

        ProgramaAst ast = new ProgramaAst(
                TipoModulo.PROGRAMA,
                "TesteGitAudit",
                "2.4.0",
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(regraComRastreio, regraSemRastreio),
                List.of()
        );

        ThzGitAuditEngine.RelatorioGitAudit relatorio = ThzGitAuditEngine.auditarGit(ast, ".");

        assertNotNull(relatorio);
        assertTrue(relatorio.requisitosImpactados().stream().anyMatch(r -> r.contains("REQ-FIN-100")));
        assertFalse(relatorio.alertasGovernanca().isEmpty());
    }
}
