package thz.lang.documento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LivroManualPdfTest {

    @Test
    @DisplayName("Deve descobrir capítulos da documentação e gerar Livro-Manual PDF com sucesso")
    void deveGerarLivroManualPdf() throws Exception {
        Path raiz = Path.of(".").toAbsolutePath().normalize();
        if (!Files.exists(raiz.resolve("docs"))) {
            if (Files.exists(raiz.resolve("../thz-lang/docs"))) {
                raiz = raiz.resolve("../thz-lang").normalize();
            } else {
                raiz = raiz.resolve("../../").normalize();
            }
        }

        var capitulos = ThzLivroManualPdf.descobrirCapitulos(raiz);
        assertFalse(capitulos.isEmpty(), "Deve descobrir pelo menos um capítulo de documentação");

        Path tempPdf = Files.createTempFile("thz-manual-test-", ".pdf");
        Path tempPdfEn = Files.createTempFile("thz-manual-en-test-", ".pdf");
        try {
            var capitulosPt = ThzLivroManualPdf.descobrirCapitulos(raiz);
            System.out.println("=== CAPITULOS PT (" + capitulosPt.size() + ") ===");
            for (var c : capitulosPt) {
                System.out.println("  " + c.parte() + " -> " + c.titulo() + " (" + c.arquivo().getFileName() + ")");
            }

            // PT-BR
            Path resultado = ThzLivroManualPdf.gerarLivroManual(raiz, tempPdf, thz.lang.lexico.DialetoLinguagem.PT_BR);
            assertTrue(Files.exists(resultado), "O arquivo PDF do livro-manual em PT-BR deve existir");
            assertTrue(Files.size(resultado) > 1000, "O PDF PT-BR deve conter conteúdo substancial");

            // EN-US
            var capitulosEn = ThzLivroManualPdf.descobrirCapitulosDinamicamente(raiz, thz.lang.lexico.DialetoLinguagem.EN_US);
            System.out.println("=== CAPITULOS EN (" + capitulosEn.size() + ") ===");
            for (var c : capitulosEn) {
                System.out.println("  " + c.parte() + " -> " + c.titulo() + " (" + c.arquivo().getFileName() + ")");
            }
            assertFalse(capitulosEn.isEmpty());
            assertTrue(capitulosEn.stream().anyMatch(c -> c.arquivo().toString().contains("en")),
                    "Capítulos em inglês devem apontar para a pasta docs/en");

            Path resultadoEn = ThzLivroManualPdf.gerarLivroManual(raiz, tempPdfEn, thz.lang.lexico.DialetoLinguagem.EN_US);
            assertTrue(Files.exists(resultadoEn), "O arquivo PDF do livro-manual em EN-US deve existir");
            assertTrue(Files.size(resultadoEn) > 1000, "O PDF EN-US deve conter conteúdo substancial");
        } finally {
            Files.deleteIfExists(tempPdf);
            Files.deleteIfExists(tempPdfEn);
        }
    }
}

