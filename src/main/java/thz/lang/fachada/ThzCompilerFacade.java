package thz.lang.fachada;

import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoHelper;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.docgen.ThzDocGen;
import thz.lang.formato.Formatador;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ThzCompilerFacade — Fachada unificada para o pipeline de compilação/análise da THZ-LANG.
 * Elimina a duplicação entre API, LSP, CLI e GUI.
 *
 * Fornece: análise léxico/sintático/semântica, formatação, documentação,
 * auditoria, IR, LLVM, SIMD, extração de símbolos e hover.
 */
public final class ThzCompilerFacade {

    private ThzCompilerFacade() {}

    // ---- Registros de resultado ----

    public record Diagnostico(int linha, int coluna, String mensagem, String origem, String severidade) {}

    public record Simbolo(String nome, String categoria, String detalhe, int linha, int coluna, String container) {}

    public record ResultadoAnalise(
            ProgramaAst ast,
            List<Diagnostico> diagnosticos,
            List<String> textoDiagnosticos,
            boolean temErros,
            List<Simbolo> simbolos
    ) {}

    public record ResultadoFormatacao(String resultado, boolean alterou) {}

    public record ResultadoAuditoria(String markdown, String json) {}

    public record ResultadoIr(String json, String llvm) {}

    public record ResultadoSimd(List<ResultadoValidacaoSimd> resultados) {}

    public record HoverInfo(String conteudo, int linha, int colunaInicio, int colunaFim) {}

    // ---- Pipeline de análise ----

    /**
     * Pipeline completo: léxico -> sintático -> semântico.
     * Retorna AST + diagnósticos unificados + símbolos.
     */
    public static ResultadoAnalise analisar(String fonte, boolean estrito) {
        List<Diagnostico> diagnosticos = new ArrayList<>();
        ProgramaAst ast = null;

        // 1) Léxico
        List<Token> tokens;
        ThzLexer lexer;
        try {
            lexer = new ThzLexer(fonte);
            tokens = lexer.tokenize();
        } catch (Exception e) {
            DiagnosticoHelper.Diagnostico d = DiagnosticoHelper.fromExcecao(e, "lexico");
            diagnosticos.add(new Diagnostico(d.linha(), d.coluna(), d.mensagem(), d.origem(), d.severidade()));
            return new ResultadoAnalise(null, diagnosticos, List.of(), true, List.of());
        }

        // 2) Sintático
        try {
            ast = new ThzParser(tokens, lexer.getDialeto()).parse();
        } catch (Exception e) {
            DiagnosticoHelper.Diagnostico d = DiagnosticoHelper.fromExcecao(e, "sintatico");
            diagnosticos.add(new Diagnostico(d.linha(), d.coluna(), d.mensagem(), d.origem(), d.severidade()));
            List<String> textoDiags = Diagnosticos.formatarDiagnosticos(fonte,
                    diagnosticos.stream().map(d2 -> new DiagnosticoEntrada(d2.linha(), d2.coluna(), d2.mensagem())).toList(),
                    "");
            return new ResultadoAnalise(null, diagnosticos, textoDiags, true, List.of());
        }

        // 3) Semântico
        List<ErroSemantico> errosSemanticos = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
        for (DiagnosticoHelper.Diagnostico d : DiagnosticoHelper.fromErrosSemanticos(errosSemanticos)) {
            diagnosticos.add(new Diagnostico(d.linha(), d.coluna(), d.mensagem(), d.origem(), d.severidade()));
        }

        boolean temErros = !diagnosticos.isEmpty();
        List<String> textoDiags = temErros
                ? Diagnosticos.formatarDiagnosticos(fonte,
                        diagnosticos.stream().map(d -> new DiagnosticoEntrada(d.linha(), d.coluna(), d.mensagem())).toList(),
                        "")
                : List.of();

        List<Simbolo> simbolos = extrairSimbolos(ast, tokens);

        return new ResultadoAnalise(ast, diagnosticos, textoDiags, temErros, simbolos);
    }

    // ---- Conveniências de parsing ----

    /**
     * Parse léxico + sintático. Retorna null se houver erro.
     */
    public static ProgramaAst parseAst(String fonte) {
        try {
            ThzLexer lexer = new ThzLexer(fonte);
            List<Token> tokens = lexer.tokenize();
            return new ThzParser(tokens, lexer.getDialeto()).parse();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Formatação ----

    /**
     * Formata o código fonte THZ de forma canônica e idempotente.
     */
    public static ResultadoFormatacao formatar(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new ResultadoFormatacao(fonte, false);
        String fmt = Formatador.formatarEnxuto(ast);
        return new ResultadoFormatacao(fmt, !fmt.equals(fonte));
    }

    // ---- Documentação ----

    /**
     * Gera documentação Markdown + Mermaid a partir do código fonte.
     */
    public static String gerarDocumentacao(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return null;
        return ThzDocGen.gerarDocumentacao(ast);
    }

    // ---- Auditoria ----

    /**
     * Realiza auditoria de governança (G4) e retorna markdown + JSON.
     */
    public static ResultadoAuditoria auditar(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new ResultadoAuditoria(null, null);
        RelatorioAuditoria relatorio = AuditorGovernanca.auditar(ast);
        String markdown = AuditorGovernanca.gerarMarkdownGovernanca(relatorio);
        String json = serializarRelatorio(relatorio);
        return new ResultadoAuditoria(markdown, json);
    }

    // ---- IR ----

    /**
     * Gera a representação intermediária THZ-IR/1.
     */
    public static ResultadoIr gerarIr(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new ResultadoIr(null, null);
        IrPrograma ir = GeradorIr.baixarParaIr(ast);
        String json = GeradorIr.serializarIr(ir);
        return new ResultadoIr(json, null);
    }

    // ---- LLVM ----

    /**
     * Emite LLVM IR a partir do código fonte.
     */
    public static String emitirLlvm(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return null;
        return GeradorIr.emitirLlvm(ast);
    }

    // ---- SIMD ----

    /**
     * Valida regras de vetorização SIMD (R1-R5).
     */
    public static ResultadoSimd validarSimd(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new ResultadoSimd(List.of());
        return new ResultadoSimd(ValidadorSimd.analisarTudo(ast));
    }

    // ---- Símbolos ----

    /**
     * Extrai símbolos da AST para indexação/hover.
     */
    public static List<Simbolo> extrairSimbolos(ProgramaAst ast, List<Token> tokens) {
        List<Simbolo> simbolos = new ArrayList<>();

        Map<String, List<int[]>> posPorNome = tokens.stream()
                .filter(t -> t != null && t.type() == TokenType.IDENTIFICADOR)
                .collect(Collectors.groupingBy(
                        Token::value,
                        Collectors.mapping(t -> new int[]{t.line(), t.column()}, Collectors.toList())
                ));

        Function<String, int[]> primeiraPos = (String nome) -> {
            List<int[]> lista = posPorNome.get(nome);
            return (lista != null && !lista.isEmpty()) ? lista.getFirst() : new int[]{1, 1};
        };

        // Módulo / Programa
        {
            int[] p = primeiraPos.apply(ast.nome());
            String detalhe = ast.tipoModulo() != null ? ast.tipoModulo().descricao() : "PROGRAMA";
            simbolos.add(new Simbolo(ast.nome(), "programa", detalhe, p[0], p[1], null));
        }

        // Estruturas
        if (ast.estruturas() != null) {
            for (var e : ast.estruturas()) {
                int[] p = primeiraPos.apply(e.nome());
                simbolos.add(new Simbolo(e.nome(), "estrutura", e.layoutColunar() ? "LAYOUT_COLUNAR" : null, p[0], p[1], null));
                if (e.campos() != null) {
                    for (var c : e.campos()) {
                        int[] pc = primeiraPos.apply(c.nome());
                        simbolos.add(new Simbolo(c.nome(), "campo", c.tipo(), pc[0], pc[1], e.nome()));
                    }
                }
            }
        }

        // Enumerações
        if (ast.enumeracoes() != null) {
            for (var en : ast.enumeracoes()) {
                int[] p = primeiraPos.apply(en.nome());
                simbolos.add(new Simbolo(en.nome(), "enumeracao", null, p[0], p[1], null));
                if (en.membros() != null) {
                    for (String m : en.membros()) {
                        int[] pm = primeiraPos.apply(m);
                        simbolos.add(new Simbolo(m, "membro-enum", null, pm[0], pm[1], en.nome()));
                    }
                }
            }
        }

        // Regras de negócio
        if (ast.regras() != null) {
            for (var regra : ast.regras()) {
                int[] pr = primeiraPos.apply(regra.nome());
                simbolos.add(new Simbolo(regra.nome(), "regra", regra.identificador(), pr[0], pr[1], null));
                if (regra.operacoes() != null) {
                    for (var op : regra.operacoes()) {
                        int[] po = primeiraPos.apply(op.nome());
                        String assinatura = op.parametros() != null
                                ? op.parametros().stream().map(p -> p.nome() + ": " + p.tipo()).collect(Collectors.joining(", "))
                                : "";
                        assinatura += " : " + op.tipoRetorno();
                        simbolos.add(new Simbolo(op.nome(), "operacao", assinatura, po[0], po[1], regra.nome()));
                        if (op.parametros() != null) {
                            for (var param : op.parametros()) {
                                int[] pp = primeiraPos.apply(param.nome());
                                simbolos.add(new Simbolo(param.nome(), "parametro", param.tipo(), pp[0], pp[1], op.nome()));
                            }
                        }
                    }
                }
            }
        }

        // Funções reutilizáveis
        if (ast.funcoes() != null) {
            for (var funcao : ast.funcoes()) {
                int[] pf = primeiraPos.apply(funcao.nome());
                String assinatura = funcao.parametros() != null
                        ? funcao.parametros().stream().map(p -> p.nome() + ": " + p.tipo()).collect(Collectors.joining(", "))
                        : "";
                assinatura += " : " + funcao.tipoRetorno();
                simbolos.add(new Simbolo(funcao.nome(), "funcao", assinatura, pf[0], pf[1], null));
                if (funcao.parametros() != null) {
                    for (var param : funcao.parametros()) {
                        int[] pp = primeiraPos.apply(param.nome());
                        simbolos.add(new Simbolo(param.nome(), "parametro", param.tipo(), pp[0], pp[1], funcao.nome()));
                    }
                }
            }
        }

        return simbolos;
    }

    // ---- Hover ----

    /**
     * Resolve hover (tipo/assinatura) para a posição informada no código.
     */
    public static HoverInfo obterHover(String fonte, int linha, int coluna) {
        ResultadoAnalise r = analisar(fonte, false);
        if (r.ast() == null || r.simbolos().isEmpty()) return null;

        String[] linhas = fonte.split("\\r?\\n", -1);
        if (linha - 1 >= linhas.length || linha - 1 < 0) return null;
        String conteudo = linhas[linha - 1];
        int idx = Math.max(0, Math.min(coluna - 1, conteudo.length()));
        if (idx >= conteudo.length()) return null;

        char c = conteudo.charAt(idx);
        if (!Character.isLetterOrDigit(c) && c != '_') return null;

        int ini = idx;
        int fim = idx;
        while (ini > 0 && (Character.isLetterOrDigit(conteudo.charAt(ini - 1)) || conteudo.charAt(ini - 1) == '_')) ini--;
        while (fim < conteudo.length() && (Character.isLetterOrDigit(conteudo.charAt(fim)) || conteudo.charAt(fim) == '_')) fim++;

        String palavra = conteudo.substring(ini, fim);
        if (palavra.isBlank()) return null;

        for (Simbolo s : r.simbolos()) {
            if (s.nome().equals(palavra)) {
                String conteudoHover = formatarHover(s, palavra);
                return new HoverInfo(conteudoHover, linha, ini + 1, fim + 1);
            }
        }
        return null;
    }

    // ---- Helpers públicos ----

    // ---- Helpers internos ----

    private static String formatarHover(Simbolo s, String palavra) {
        return switch (s.categoria()) {
            case "estrutura" -> "**ESTRUTURA** `" + palavra + "`";
            case "campo" -> "**campo** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "` — em `" + s.container() + "`";
            case "enumeracao" -> "**ENUMERACAO** `" + palavra + "`";
            case "membro-enum" -> "**membro** `" + palavra + "` : `" + (s.container() != null ? s.container() : "ENUMERACAO") + "`";
            case "regra" -> "**REGRA_NEGOCIO** `" + palavra + "`";
            case "operacao" -> "**OPERACAO** `" + palavra + "(" + (s.detalhe() != null ? s.detalhe() : "") + ")` — em `" + (s.container() != null ? s.container() : "") + "`";
            case "funcao" -> "**FUNCAO** `" + palavra + "(" + (s.detalhe() != null ? s.detalhe() : "") + ")`";
            case "parametro" -> "**parametro** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "`";
            case "variavel" -> "**variavel** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "`";
            case "programa" -> "**PROGRAMA** `" + palavra + "`";
            default -> "**" + s.categoria() + "** `" + palavra + "`";
        };
    }

    private static String serializarRelatorio(RelatorioAuditoria r) {
        if (r == null) return "null";
        return "{" +
                "\"nomePrograma\":\"" + escape(r.nomePrograma()) + "\"," +
                "\"metricas\":{" +
                "\"totalRegras\":" + r.metricas().totalRegras() + "," +
                "\"regrasComRastreio\":" + r.metricas().regrasComRastreio() + "," +
                "\"totalContratosExige\":" + r.metricas().totalContratosExige() + "," +
                "\"totalContratosGarante\":" + r.metricas().totalContratosGarante() + "," +
                "\"totalInvariantes\":" + r.metricas().totalInvariantes() + "," +
                "\"totalOperacoesIdempotentes\":" + r.metricas().totalOperacoesIdempotentes() + "," +
                "\"percentualConformidade\":" + r.metricas().percentualConformidade() + "," +
                "\"aprovado\":" + r.metricas().aprovado() +
                "}}";
    }

    private static String escape(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
