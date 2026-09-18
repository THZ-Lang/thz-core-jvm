package thz.lang.driver;

import thz.lang.ast.ProgramaAst;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.js.ThzJsEmitter;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;

import java.util.List;
import java.util.Map;

/**
 * ThzCompilerDriver — Driver unificado para orquestração de compilação, análise, IR e alvos de execução.
 */
public final class ThzCompilerDriver {

    public enum Alvo {
        EXECUCAO_JVM,
        THZ_IR,
        LLVM,
        JAVASCRIPT,
        AUDITORIA,
        WEBASSEMBLY
    }

    public record ResultadoCompilacao(
            boolean sucesso,
            ProgramaAst ast,
            List<ErroSemantico> erros,
            String saidaTexto,
            ValorThz resultadoExecucao
    ) {}

    private ThzCompilerDriver() {}

    public static ResultadoCompilacao compilarOuExecutar(String fonte, Alvo alvo, boolean modoEstrito, Map<String, ValorThz> argumentos) {
        // 1. Léxico & Sintático
        ThzLexer lexer = new ThzLexer(fonte);
        List<Token> tokens = lexer.tokenize();
        ProgramaAst ast = new ThzParser(tokens, lexer.getDialeto()).parse();

        // 2. Semântico
        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        List<ErroSemantico> erros = semantico.analisar(new OpcoesAnalise(modoEstrito));
        if (!erros.isEmpty()) {
            return new ResultadoCompilacao(false, ast, erros, null, null);
        }

        // 3. Despacho por Alvo
        return switch (alvo) {
            case EXECUCAO_JVM -> {
                InterpretadorThz interp = new InterpretadorThz(ast);
                ValorThz ret = ValorThz.NULO;
                if (!interp.listarOperacoesExecutaveis().isEmpty()) {
                    String opPrincipal = interp.listarOperacoesExecutaveis().get(0).operacao().nome();
                    ret = interp.executarOperacao(opPrincipal, argumentos != null ? argumentos : Map.of());
                } else if (!interp.listarProcedimentos().isEmpty()) {
                    String proc = interp.listarProcedimentos().get(0).nome();
                    interp.executarProcedimento(proc, argumentos != null ? argumentos : Map.of());
                }
                yield new ResultadoCompilacao(true, ast, List.of(), null, ret);
            }
            case THZ_IR -> {
                IrPrograma ir = GeradorIr.baixarParaIr(ast);
                yield new ResultadoCompilacao(true, ast, List.of(), GeradorIr.serializarIrJson(ir), null);
            }
            case LLVM -> {
                if (modoEstrito) {
                    List<String> limitacoes = GeradorIr.validarCapacidadeLlvm(ast);
                    if (!limitacoes.isEmpty()) {
                        List<ErroSemantico> errosBackend = limitacoes.stream()
                                .map(msg -> new ErroSemantico(1, 1, "[Backend LLVM] " + msg))
                                .toList();
                        yield new ResultadoCompilacao(false, ast, errosBackend, null, null);
                    }
                }
                String llvm = GeradorIr.emitirLlvm(ast, modoEstrito);
                yield new ResultadoCompilacao(true, ast, List.of(), llvm, null);
            }
            case JAVASCRIPT -> {
                String js = ThzJsEmitter.emitir(ast);
                yield new ResultadoCompilacao(true, ast, List.of(), js, null);
            }
            case AUDITORIA -> {
                RelatorioAuditoria relatorio = AuditorGovernanca.auditar(ast);
                String md = AuditorGovernanca.gerarMarkdownGovernanca(relatorio);
                yield new ResultadoCompilacao(true, ast, List.of(), md, null);
            }
            case WEBASSEMBLY -> {
                ErroSemantico erroWasm = new ErroSemantico(
                        1, 1,
                        "[Backend WebAssembly] O compilador JVM não gera bytecode binário WebAssembly (.wasm) diretamente. " +
                        "A compilação WASM requer o pipeline nativo Rust/AOT ou o emissor JavaScript (Alvo.JAVASCRIPT).");
                yield new ResultadoCompilacao(false, ast, List.of(erroWasm), null, null);
            }
        };
    }
}
