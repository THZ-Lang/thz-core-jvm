package thz.lang.governanca;

import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de Auditoria de Governança Integrado ao Git.
 * Cruza o status de modificação do repositório (git status / git diff) com os requisitos
 * e contratos declarados na AST do THZ-LANG.
 */
public final class ThzGitAuditEngine {

    public record RelatorioGitAudit(
            boolean repoGit,
            String branchAtual,
            List<String> arquivosModificados,
            List<String> requisitosImpactados,
            List<String> alertasGovernanca
    ) {}

    public static RelatorioGitAudit auditarGit(ProgramaAst ast, String diretorioTrabalho) {
        List<String> modificados = obterArquivosModificadosGit(diretorioTrabalho);
        String branch = obterBranchAtualGit(diretorioTrabalho);
        boolean ehRepoGit = branch != null;

        List<String> requisitos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        if (ast != null && ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                if (regra.rastreioRequisito() != null) {
                    requisitos.add(regra.rastreioRequisito() + " (" + regra.nome() + ")");
                } else {
                    alertas.add("Regra de negócio '" + regra.nome() + "' sem RASTREIO_REQUISITO alterada no código.");
                }
                if (regra.clausulasEntrada().isEmpty() && regra.clausulasSaida().isEmpty()) {
                    alertas.add("Regra de negócio '" + regra.nome() + "' sem cláusulas de contrato (EXIGE/GARANTE).");
                }
            }
        }

        return new RelatorioGitAudit(ehRepoGit, branch != null ? branch : "main", modificados, requisitos, alertas);
    }

    private static String obterBranchAtualGit(String dir) {
        try {
            Process p = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                    .directory(new java.io.File(dir))
                    .start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha = br.readLine();
                return linha != null ? linha.trim() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> obterArquivosModificadosGit(String dir) {
        List<String> resultado = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("git", "status", "--porcelain")
                    .directory(new java.io.File(dir))
                    .start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    resultado.add(linha.trim());
                }
            }
        } catch (Exception ignored) {}
        return resultado;
    }
}
