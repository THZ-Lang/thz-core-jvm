package thz.lang.fachada;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThzCompilerFacadeFuncaoTest {
    @Test
    void deveExporFuncaoComoSimboloEHover() {
        String fonte = """
                PROGRAMA Simbolos
                FUNCAO dobrar(valor: INTEIRO32): INTEIRO32
                    RETORNE valor * 2
                FIM_FUNCAO
                FIM_PROGRAMA
                """;

        var analise = ThzCompilerFacade.analisar(fonte, false);
        assertFalse(analise.temErros());
        assertTrue(analise.simbolos().stream().anyMatch(s -> s.nome().equals("dobrar") && s.categoria().equals("funcao")));
        var hover = ThzCompilerFacade.obterHover(fonte, 2, 8);
        assertNotNull(hover);
        assertTrue(hover.conteudo().contains("FUNCAO"));
    }
}
