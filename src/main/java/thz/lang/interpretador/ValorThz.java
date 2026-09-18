package thz.lang.interpretador;

import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Universo de valores THZ — port exato de interpretador.ts ValorThz.
 * Sealed interface com records para cada classe THZ.
 */
public sealed interface ValorThz permits
        ValorThz.Inteiro,
        ValorThz.Decimal,
        ValorThz.Monetario,
        ValorThz.Texto,
        ValorThz.Logico,
        ValorThz.Nulo,
        ValorThz.Enumerado,
        ValorThz.Resultado,
        ValorThz.Registro,
        ValorThz.Fatia,
        ValorThz.Data,
        ValorThz.DataHora {

    record Inteiro(BigInteger valor) implements ValorThz {}
    record Decimal(DecimalFixo valor) implements ValorThz {}
    record Monetario(thz.lang.runtime.Monetario valor) implements ValorThz {}
    record Texto(String valor) implements ValorThz {}
    record Logico(boolean valor) implements ValorThz {}
    record Nulo() implements ValorThz {}
    record Enumerado(String nomeEnumeracao, String valor) implements ValorThz {}
    record Resultado(boolean sucesso, ValorThz valor, ValorThz erro) implements ValorThz {}
    record Registro(String nomeEstrutura, Map<String, ValorThz> campos) implements ValorThz {}
    record Fatia(String tipoInterno, List<ValorThz> elementos) implements ValorThz {}
    record Data(DataThz valor) implements ValorThz {}
    record DataHora(DataHoraThz valor) implements ValorThz {}

    // Singleton NULO
    Nulo NULO = new Nulo();

    // Helpers de fábrica — espelham TS INTEIRO/DECIMAL/etc
    static Inteiro INTEIRO(BigInteger v) { return new Inteiro(v); }
    static Inteiro INTEIRO(long v) { return new Inteiro(BigInteger.valueOf(v)); }
    static Decimal DECIMAL(DecimalFixo v) { return new Decimal(v); }
    static Decimal DECIMAL(String v) {
        int dot = v.indexOf('.');
        int escala = dot >= 0 ? v.length() - dot - 1 : 2;
        return new Decimal(DecimalFixo.deTexto(v, Math.max(2, escala)));
    }
    static Monetario MONETARIO(thz.lang.runtime.Monetario v) { return new Monetario(v); }
    static Texto TEXTO(String v) { return new Texto(v); }
    static Logico LOGICO(boolean v) { return new Logico(v); }
    static Enumerado ENUMERADO(String nomeEnum, String v) { return new Enumerado(nomeEnum, v); }
    static Data DATA(DataThz v) { return new Data(v); }
    static DataHora DATA_HORA(DataHoraThz v) { return new DataHora(v); }
    static Fatia FATIA(List<ValorThz> elementos) { return new Fatia("QUALQUER", elementos); }

    /** Retorna nome da classe THZ para mensagens de erro com verificação exaustiva em tempo de compilação. */
    default String classe() {
        return switch (this) {
            case Inteiro _ -> "INTEIRO";
            case Decimal _ -> "DECIMAL";
            case Monetario _ -> "MONETARIO";
            case Texto _ -> "TEXTO";
            case Logico _ -> "LOGICO";
            case Nulo _ -> "NULO";
            case Enumerado _ -> "ENUMERADO";
            case Resultado _ -> "RESULTADO";
            case Registro _ -> "REGISTRO";
            case Fatia _ -> "FATIA";
            case Data _ -> "DATA";
            case DataHora _ -> "DATA_HORA";
        };
    }

    /** Retorna representação textual amigável do valor THZ. */
    default String formatar() {
        return switch (this) {
            case Inteiro i -> i.valor().toString();
            case Decimal d -> d.valor().formatar();
            case Monetario m -> m.valor().formatar();
            case Texto t -> t.valor();
            case Logico l -> l.valor() ? "VERDADEIRO" : "FALSO";
            case Nulo _ -> "NULO";
            case Enumerado e -> e.valor();
            case Resultado r -> r.sucesso() ? "SUCESSO(" + r.valor().formatar() + ")" : "ERRO(" + r.erro().formatar() + ")";
            case Registro reg -> {
                StringBuilder sb = new StringBuilder(reg.nomeEstrutura()).append("{");
                boolean prim = true;
                for (Map.Entry<String, ValorThz> entry : reg.campos().entrySet()) {
                    if (!prim) sb.append(", ");
                    prim = false;
                    sb.append(entry.getKey()).append(": ").append(entry.getValue() != null ? entry.getValue().formatar() : "NULO");
                }
                sb.append("}");
                yield sb.toString();
            }
            case Fatia f -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < f.elementos().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(f.elementos().get(i) != null ? f.elementos().get(i).formatar() : "NULO");
                }
                sb.append("]");
                yield sb.toString();
            }
            case Data dt -> dt.valor().formatar();
            case DataHora dh -> dh.valor().formatar();
        };
    }
}

