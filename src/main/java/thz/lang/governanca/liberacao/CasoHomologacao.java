package thz.lang.governanca.liberacao;

/**
 * Representa um caso de homologação executável para liberação de binário para produção.
 * Em conformidade com a Especificação Seção 6 e Critério P01:
 * Um caso propositalmente inválido passa se for rejeitado conforme o resultado esperado.
 */
public record CasoHomologacao(
        String identificador,
        String descricao,
        boolean esperaSucesso,
        String erroEsperadoContem,
        RunnableCaso executor
) {
    @FunctionalInterface
    public interface RunnableCaso {
        void executar() throws Exception;
    }

    public static CasoHomologacao casoPositivo(String id, String desc, RunnableCaso executor) {
        return new CasoHomologacao(id, desc, true, null, executor);
    }

    public static CasoHomologacao casoNegativo(String id, String desc, String erroEsperado, RunnableCaso executor) {
        return new CasoHomologacao(id, desc, false, erroEsperado, executor);
    }
}
