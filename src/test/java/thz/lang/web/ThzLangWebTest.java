package thz.lang.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.Monetario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzLangWebTest {

    @Test
    @DisplayName("ThzLangWeb deve serializar tipos THZ para JSON com precisão e estrutura correta")
    void testSerializacaoJsonTipos() {
        // Primitivos
        assertEquals("\"olá\"", ThzLangWeb.serializarThzParaJson(ValorThz.TEXTO("olá")));
        assertEquals("100", ThzLangWeb.serializarThzParaJson(ValorThz.INTEIRO(100)));
        assertEquals("true", ThzLangWeb.serializarThzParaJson(ValorThz.LOGICO(true)));
        assertEquals("null", ThzLangWeb.serializarThzParaJson(ValorThz.NULO));

        // Decimal & Monetário
        assertEquals("150.7500", ThzLangWeb.serializarThzParaJson(ValorThz.DECIMAL(DecimalFixo.deTexto("150.75", 4))));
        assertTrue(ThzLangWeb.serializarThzParaJson(ValorThz.MONETARIO(Monetario.deTexto("99.90", "BRL"))).contains("BRL"));

        // Data
        assertEquals("\"2026-08-24\"", ThzLangWeb.serializarThzParaJson(ValorThz.DATA(DataThz.deTexto("2026-08-24"))));

        // Fatia
        ValorThz.Fatia fatia = new ValorThz.Fatia("INTEIRO", List.of(ValorThz.INTEIRO(1), ValorThz.INTEIRO(2), ValorThz.INTEIRO(3)));
        assertEquals("[1,2,3]", ThzLangWeb.serializarThzParaJson(fatia));

        // Registro
        Map<String, ValorThz> campos = new LinkedHashMap<>();
        campos.put("id", ValorThz.TEXTO("CLI-001"));
        campos.put("limite", ValorThz.DECIMAL(DecimalFixo.deTexto("5000.00", 2)));
        ValorThz.Registro reg = new ValorThz.Registro("Cliente", campos);
        String jsonReg = ThzLangWeb.serializarThzParaJson(reg);
        assertTrue(jsonReg.contains("\"id\":\"CLI-001\""));
        assertTrue(jsonReg.contains("\"limite\":5000.00"));

        // Resultado SUCESSO e ERRO
        ValorThz.Resultado resSucesso = new ValorThz.Resultado(true, ValorThz.TEXTO("OK"), null);
        assertTrue(ThzLangWeb.serializarThzParaJson(resSucesso).contains("\"status\":\"SUCESSO\""));

        ValorThz.Resultado resErro = new ValorThz.Resultado(false, null, ValorThz.TEXTO("Falha"));
        assertTrue(ThzLangWeb.serializarThzParaJson(resErro).contains("\"status\":\"ERRO\""));
    }
}
