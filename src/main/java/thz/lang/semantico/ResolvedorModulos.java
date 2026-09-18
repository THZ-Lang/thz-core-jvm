package thz.lang.semantico;

import thz.lang.ast.ImportacaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolvedor de módulos e importações do THZ-LANG.
 */
public class ResolvedorModulos {

    private final Map<String, ProgramaAst> cache = new HashMap<>();
    private final Map<String, String> modulosVirtuais = new HashMap<>();

    public void registrarModuloVirtual(String nomeOuCaminho, String codigoFonte) {
        Objects.requireNonNull(nomeOuCaminho, "O nome do módulo não pode ser nulo");
        Objects.requireNonNull(codigoFonte, "O código-fonte do módulo não pode ser nulo");
        modulosVirtuais.put(nomeOuCaminho, codigoFonte);
    }

    public ProgramaAst resolver(ImportacaoAst importacao, Path diretorioBase) {
        if (importacao == null) return null;
        String chave = importacao.caminho() != null ? importacao.caminho() : importacao.modulo();
        if (cache.containsKey(chave)) {
            return cache.get(chave);
        }

        String fonte = null;
        if (modulosVirtuais.containsKey(chave)) {
            fonte = modulosVirtuais.get(chave);
        } else if (modulosVirtuais.containsKey(importacao.modulo())) {
            fonte = modulosVirtuais.get(importacao.modulo());
        } else {
            String termo = importacao.caminho() != null ? importacao.caminho() : importacao.modulo();
            var encontrado = thz.lang.io.ThzLocalizadorRecursos.localizarModulo(termo, diretorioBase);
            if (encontrado.isPresent()) {
                try {
                    fonte = Files.readString(encontrado.get());
                } catch (IOException e) {
                    throw new IllegalStateException("Erro ao ler arquivo de importação: " + encontrado.get(), e);
                }
            }
        }

        if (fonte == null) {
            return null;
        }

        ProgramaAst ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
        cache.put(chave, ast);
        return ast;
    }
}
