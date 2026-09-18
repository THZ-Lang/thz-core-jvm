package thz.lang.tools;

import thz.lang.ast.ImportacaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;

/**
 * ThzLangTools — Conjunto de ferramentas de diagnóstico arquitetural, linter e inspeção de dependências.
 */
public final class ThzLangTools {

    public record DiagnosticoLinter(String nivel, String mensagem, int linha, int coluna) {}

    private ThzLangTools() {}

    public static List<DiagnosticoLinter> executarLint(String codigoFonte) {
        List<DiagnosticoLinter> diagnosticos = new ArrayList<>();
        ProgramaAst ast = new ThzParser(new ThzLexer(codigoFonte).tokenize()).parse();

        // 1. Verificação de Metadados de Arquitetura
        if (ast.metadados() == null) {
            diagnosticos.add(new DiagnosticoLinter("AVISO", "Programa sem bloco METADADOS_ARQUITETURA (recomendado para rastreabilidade corporativa).", 1, 1));
        }

        // 2. Verificação de Contratos Formais em Regras de Negócio
        if (ast.regras() != null) {
            for (RegraNegocioAst r : ast.regras()) {
                if (r.clausulasEntrada().isEmpty() && r.clausulasSaida().isEmpty()) {
                    diagnosticos.add(new DiagnosticoLinter("INFO", "Regra de negócio '" + r.nome() + "' sem cláusulas formais EXIGE ou GARANTE.", 1, 1));
                }
            }
        }

        // 3. Verificação de Estruturas sem Invariantes
        if (ast.estruturas() != null) {
            for (var est : ast.estruturas()) {
                if (est.invariantes().isEmpty()) {
                    diagnosticos.add(new DiagnosticoLinter("INFO", "Estrutura '" + est.nome() + "' não declara INVARIANTE de autoproteção de domínio.", 1, 1));
                }
            }
        }

        return diagnosticos;
    }

    public static List<String> obterGrafoDependencias(ProgramaAst ast) {
        List<String> dependencias = new ArrayList<>();
        if (ast.importacoes() != null) {
            for (ImportacaoAst imp : ast.importacoes()) {
                dependencias.add(imp.modulo() + (imp.caminho() != null ? " (" + imp.caminho() + ")" : ""));
            }
        }
        return dependencias;
    }
}
