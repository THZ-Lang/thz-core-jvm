package thz.lang.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.ast.ImportacaoAst;
import thz.lang.semantico.ResolvedorModulos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThzLocalizadorRecursosTest {

    @TempDir
    Path tempWorkspace;

    @Test
    @DisplayName("Deve localizar arquivo recursivamente em subpastas aninhadas sem extensão")
    void testLocalizarArquivoRecursivoSemExtensao() throws IOException {
        // Cria estrutura aninhada: tempWorkspace/src/servicos/financeiro/faturamento_anual.thz
        Path subpasta = tempWorkspace.resolve("src").resolve("servicos").resolve("financeiro");
        Files.createDirectories(subpasta);
        Path arqFonte = subpasta.resolve("faturamento_anual.thz");
        Files.writeString(arqFonte, "PROGRAMA FaturamentoAnual\nFIM_PROGRAMA\n");

        // Executa busca a partir da raiz do workspace apenas pelo nome sem extensão
        var resultado = ThzLocalizadorRecursos.localizarArquivo("faturamento_anual", tempWorkspace, List.of(".thz"));

        assertTrue(resultado.isPresent(), "O localizador DEVE encontrar o arquivo na árvore recursiva!");
        assertEquals(arqFonte.toAbsolutePath().normalize(), resultado.get().toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Deve subir hierarquia até a raiz do projeto para encontrar arquivo de configuração")
    void testSubidaHierarquicaRaizProjeto() throws IOException {
        // Cria thz.config.json na raiz do workspace
        Path configRaiz = tempWorkspace.resolve("thz.config.json");
        Files.writeString(configRaiz, "{\"projeto\": {\"nome\": \"AppRecursivo\"}}");

        // Cria uma pasta filha profunda
        Path subpastaProfunda = tempWorkspace.resolve("modulos").resolve("core").resolve("internal");
        Files.createDirectories(subpastaProfunda);

        // A partir da pasta profunda, o localizador deve subir e encontrar o config na raiz
        var resultado = ThzLocalizadorRecursos.localizarArquivo("thz.config.json", subpastaProfunda, List.of(".json"));

        assertTrue(resultado.isPresent(), "O localizador DEVE subir a árvore e encontrar o manifesto na raiz!");
        assertEquals(configRaiz.toAbsolutePath().normalize(), resultado.get().toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("ResolvedorModulos deve importar módulos recursivamente sem caminho exato")
    void testResolvedorModulosRecursivo() throws IOException {
        // Cria módulo utilitário em tempWorkspace/lib/matematica_financeira.thz
        Path pastaLib = tempWorkspace.resolve("lib");
        Files.createDirectories(pastaLib);
        Path moduloLib = pastaLib.resolve("matematica_financeira.thz");
        Files.writeString(moduloLib, """
                PROGRAMA MatematicaFinanceira
                ESTRUTURA TaxaJuros
                    valor: DECIMAL
                FIM_ESTRUTURA
                FIM_PROGRAMA
                """);

        // Cria programa consumidor em tempWorkspace/src/regras/
        Path pastaConsumidor = tempWorkspace.resolve("src").resolve("regras");
        Files.createDirectories(pastaConsumidor);

        ResolvedorModulos resolvedor = new ResolvedorModulos();
        ImportacaoAst importacao = new ImportacaoAst("MatematicaFinanceira", "matematica_financeira", 1, 1);

        var astModulo = resolvedor.resolver(importacao, pastaConsumidor);

        assertNotNull(astModulo, "ResolvedorModulos DEVE localizar e compilar o módulo importado recursivamente!");
        assertEquals("MatematicaFinanceira", astModulo.nome());
        assertTrue(astModulo.estruturas().stream().anyMatch(e -> e.nome().equals("TaxaJuros")));
    }

    @Test
    @DisplayName("Deve resolver e ancorar URL SQLite com diretórios pai criados automaticamente")
    void testResolucaoUrlSqlite() {
        String urlRelativa = "jdbc:sqlite:dados/producao/corporativo.db";
        String resolvida = ThzLocalizadorRecursos.resolverUrlBancoSqlite(urlRelativa, tempWorkspace);

        assertNotNull(resolvida);
        assertTrue(resolvida.startsWith("jdbc:sqlite:"));
        assertTrue(resolvida.contains("corporativo.db"));
        assertTrue(Files.exists(tempWorkspace.resolve("dados").resolve("producao")), "O diretório pai do banco DEVE ser criado automaticamente!");
    }
}
