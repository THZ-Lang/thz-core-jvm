package thz.lang.ir;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.sintatico.ThzParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IrSimdTest {

    private ProgramaAst parsear(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    public void testValidarSimdFaturamentoCanonico() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);

        List<ResultadoValidacaoSimd> resultados = ValidadorSimd.analisarTudo(ast);
        assertFalse(resultados.isEmpty());

        ResultadoValidacaoSimd r = resultados.get(0);
        assertEquals("item", r.variavel());
        assertEquals(8, r.passoSimd());
        assertTrue(r.vetorizavel());
        assertFalse(r.temViolacoes());
        assertTrue(r.regrasAtendidas().stream().anyMatch(s -> s.contains("R1")));
        assertTrue(r.regrasAtendidas().stream().anyMatch(s -> s.contains("R2")));
    }

    @Test
    public void testGerarIrFaturamento() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);

        IrPrograma ir = GeradorIr.baixarParaIr(ast);
        assertNotNull(ir);
        assertEquals("thz-ir/1", ir.versaoIr());
        assertEquals("ProcessamentoFaturamentoLote", ir.nomePrograma());
        assertFalse(ir.estruturas().isEmpty());
        assertEquals("ItemFatura", ir.estruturas().get(0).nome());
        assertTrue(ir.estruturas().get(0).layoutColunar());

        assertFalse(ir.funcoes().isEmpty());
        assertFalse(ir.loopsSimd().isEmpty());
        assertEquals(8, ir.loopsSimd().get(0).passoSimd());
        assertTrue(ir.loopsSimd().get(0).vetorizavel());

        // Serialização JSON
        String json = GeradorIr.serializarIrJson(ir);
        assertNotNull(json);
        assertTrue(json.contains("\"versaoIr\": \"thz-ir/1\""));
        assertTrue(json.contains("\"nomePrograma\": \"ProcessamentoFaturamentoLote\""));
        assertTrue(json.contains("\"layoutColunar\": true"));
    }

    @Test
    public void testEmitirLlvmFaturamento() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);

        String llvm = GeradorIr.emitirLlvm(ast);
        assertNotNull(llvm);
        assertTrue(llvm.contains("; ModuleID = 'thz.ProcessamentoFaturamentoLote'"));
        assertTrue(llvm.contains("%struct.ItemFatura = type {"));
        assertTrue(llvm.contains("define i32 @main()"));
        assertTrue(llvm.contains("@thz_arena_alloc"));
        assertTrue(llvm.contains("@thz_arena_free_all"));
    }

    @Test
    public void testViolacaoSimdComIoImpuro() {
        String fonte = """
                PROGRAMA TesteSimdInvalido
                ESTRUTURA Dado LAYOUT_COLUNAR
                    valor: INTEIRO
                FIM_ESTRUTURA
                REGRA_NEGOCIO RegraSimd
                    OPERACAO Processar(dados: FATIA[Dado]) : INTEIRO
                    INICIO
                        VETORIZAR_PARA item EM dados PASSO_SIMD 8
                            LER item.valor
                        FIM_PARA
                        RETORNE 0
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parsear(fonte);
        List<ResultadoValidacaoSimd> res = ValidadorSimd.analisarTudo(ast);
        assertFalse(res.isEmpty());
        ResultadoValidacaoSimd r = res.get(0);
        assertFalse(r.vetorizavel());
        assertTrue(r.temViolacoes());
        assertTrue(r.violacoes().stream().anyMatch(v -> v.contains("R5")));
    }
}
