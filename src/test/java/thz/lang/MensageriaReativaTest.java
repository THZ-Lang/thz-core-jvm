package thz.lang;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.mensageria.ThzBarramentoEventos;
import thz.lang.sintatico.ThzParser;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MensageriaReativaTest {

    @BeforeEach
    void setup() {
        ThzBarramentoEventos.resetar();
    }

    @Test
    @DisplayName("Deve publicar e consumir eventos com alta taxa de transferência e ordenação")
    void testPublicacaoEConsumoEventos() throws Exception {
        String topico = "pedidos.faturamento";
        int totalMensagens = 1000;

        // Produtor em Virtual Threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= totalMensagens; i++) {
                final int id = i;
                executor.submit(() -> {
                    ThzBarramentoEventos.publicar(topico, ValorThz.TEXTO("PEDIDO-" + id));
                });
            }
        }

        assertEquals(totalMensagens, ThzBarramentoEventos.tamanhoFila(topico));

        // Consumidor
        AtomicInteger consumidas = new AtomicInteger(0);
        while (ThzBarramentoEventos.tamanhoFila(topico) > 0) {
            var evt = ThzBarramentoEventos.consumir(topico, 100);
            if (evt != null) {
                consumidas.incrementAndGet();
            }
        }

        assertEquals(totalMensagens, consumidas.get());
        assertEquals(0, ThzBarramentoEventos.tamanhoFila(topico));
    }

    @Test
    @DisplayName("Deve executar programa THZ utilizando módulo MENSAGERIA")
    void testExecucaoProgramaMensageria() {
        String codigo = """
            PROGRAMA TesteMensageriaDsl
            REGRA_NEGOCIO RegraStreaming
                OPERACAO TestarFluxo() : LOGICO
                INICIO
                    VARIAVEL topico : TEXTO <- "faturas.eventos"
                    
                    # 1. Publica eventos
                    VARIAVEL off1 : INTEIRO <- MENSAGERIA.publicar(topico, "EVENTO_001")
                    VARIAVEL off2 : INTEIRO <- MENSAGERIA.publicar(topico, "EVENTO_002")
                    
                    VARIAVEL tam : INTEIRO <- MENSAGERIA.tamanhoFila(topico)
                    
                    # 2. Consome eventos
                    VARIAVEL msg1 : TEXTO <- MENSAGERIA.consumir(topico, 100)
                    VARIAVEL msg2 : TEXTO <- MENSAGERIA.consumir(topico, 100)
                    
                    SE tam = 2 E msg1 = "EVENTO_001" E msg2 = "EVENTO_002"
                        RETORNE VERDADEIRO
                    SENAO
                        RETORNE FALSO
                    FIM_SE
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("TestarFluxo", java.util.Map.of());
        assertEquals("VERDADEIRO", interp.formatar(res));
    }
}
