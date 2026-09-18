package thz.lang.ir;

import java.util.List;
import java.util.Map;

/**
 * Representação Intermediária Canônica THZ-IR (VERSAO_IR = 'thz-ir/1').
 */
public record IrPrograma(
        String versaoIr,
        String nomePrograma,
        String versaoFonte,
        Map<String, String> metadados,
        List<IrEstrutura> estruturas,
        List<IrFuncao> funcoes,
        List<IrSimdLoop> loopsSimd
) {
    public record IrEstrutura(
            String nome,
            boolean layoutColunar,
            List<IrCampo> campos
    ) {}

    public record IrCampo(
            String nome,
            String tipoPrimitivoIr,
            String tipoOriginal
    ) {}

    public record IrFuncao(
            String nome,
            String tipoRetornoIr,
            List<IrParametro> parametros,
            List<String> instrucoes,
            boolean idempotente,
            String chaveIdempotencia
    ) {
        public IrFuncao(
                String nome,
                String tipoRetornoIr,
                List<IrParametro> parametros,
                List<String> instrucoes
        ) {
            this(nome, tipoRetornoIr, parametros, instrucoes, false, null);
        }
    }

    public record IrParametro(
            String nome,
            String tipoIr
    ) {}

    public record IrSimdLoop(
            String contexto,
            String variavel,
            String fonte,
            int passoSimd,
            boolean vetorizavel
    ) {}
}
