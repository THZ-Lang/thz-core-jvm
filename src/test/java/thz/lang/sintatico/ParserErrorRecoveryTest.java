package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserErrorRecoveryTest {

    @Test
    @DisplayName("Deve expor método errosSintaticos para recuperar diagnósticos em lote")
    void testRecuperacaoDiagnosticos() {
        String codigoSintaticamenteInvalido = """
            PROGRAMA TesteErro
            VARIAVEL 12345
            PROCEDIMENTO Ok()
            INICIO
                EXIBA("Ok")
            FIM
            FIM_PROGRAMA
            """;

        List<Token> tokens = new ThzLexer(codigoSintaticamenteInvalido).tokenize();
        ThzParser parser = new ThzParser(tokens);

        assertThrows(RuntimeException.class, parser::parse);
        assertNotNull(parser.errosSintaticos());
    }
}
