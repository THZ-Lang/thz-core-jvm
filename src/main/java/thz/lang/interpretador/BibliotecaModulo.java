package thz.lang.interpretador;

import java.util.Map;

/**
 * Contrato de extensibilidade para módulos da stdlib THZ-LANG.
 * Cada módulo (TEXTO, MATEMATICA, BRASIL, etc.) implementa esta interface
 * para permitir registro dinâmico via ServiceLoader ou configuração.
 *
 * Uso:
 * <pre>
 *   public class MinhaBiblioteca implements BibliotecaModulo {
 *       public String nome() { return "MEU_MODULO"; }
 *       public void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> funcs) {
 *           BibliotecaPadrao.registrarPublico(funcs, "MEU_MODULO.foo", (args, ctx) -> { ... });
 *       }
 *   }
 * </pre>
 */
public interface BibliotecaModulo {

    /**
     * Nome do módulo (ex.: "TEXTO", "MATEMATICA", "BRASIL").
     * Usado como prefixo nas funções registradas.
     */
    String nome();

    /**
     * Registra as funções deste módulo no mapa compartilhado da stdlib.
     * Implementações devem chamar {@link BibliotecaPadrao#registrarPublico} para cada função.
     */
    void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> funcoes);
}
