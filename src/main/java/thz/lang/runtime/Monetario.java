package thz.lang.runtime;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Quantia monetária amarrada a uma moeda ISO 4217. Mistura de moedas é erro.
 * Port exato de runtime.ts — Monetario + TABELA_MOEDAS_ISO4217.
 */
public final class Monetario {

    /** record DefinicaoMoeda — definição de moeda ISO 4217 (codigo + casas) */
    public static record DefinicaoMoeda(String codigo, int casas) {}

    /** Subconjunto essencial ISO 4217; extensível sob demanda. */
    public static final Map<String, DefinicaoMoeda> TABELA_MOEDAS_ISO4217;

    static {
        Map<String, DefinicaoMoeda> m = new HashMap<>();
        // Américas / BACEN
        m.put("BRL", new DefinicaoMoeda("BRL", 2));
        m.put("USD", new DefinicaoMoeda("USD", 2));
        m.put("CAD", new DefinicaoMoeda("CAD", 2));
        m.put("MXN", new DefinicaoMoeda("MXN", 2));
        m.put("ARS", new DefinicaoMoeda("ARS", 2));
        m.put("CLP", new DefinicaoMoeda("CLP", 0));
        m.put("COP", new DefinicaoMoeda("COP", 2));
        m.put("PEN", new DefinicaoMoeda("PEN", 2));
        m.put("UYU", new DefinicaoMoeda("UYU", 2));
        m.put("PYG", new DefinicaoMoeda("PYG", 0));

        // Europa / G10
        m.put("EUR", new DefinicaoMoeda("EUR", 2));
        m.put("GBP", new DefinicaoMoeda("GBP", 2));
        m.put("CHF", new DefinicaoMoeda("CHF", 2));
        m.put("SEK", new DefinicaoMoeda("SEK", 2));
        m.put("NOK", new DefinicaoMoeda("NOK", 2));
        m.put("DKK", new DefinicaoMoeda("DKK", 2));

        // Ásia / Oceania
        m.put("JPY", new DefinicaoMoeda("JPY", 0));
        m.put("CNY", new DefinicaoMoeda("CNY", 2));
        m.put("AUD", new DefinicaoMoeda("AUD", 2));
        m.put("NZD", new DefinicaoMoeda("NZD", 2));
        m.put("INR", new DefinicaoMoeda("INR", 2));
        m.put("KRW", new DefinicaoMoeda("KRW", 0));
        m.put("SGD", new DefinicaoMoeda("SGD", 2));
        m.put("ZAR", new DefinicaoMoeda("ZAR", 2));

        // Oriente Médio (3 casas decimais)
        m.put("KWD", new DefinicaoMoeda("KWD", 3));
        m.put("BHD", new DefinicaoMoeda("BHD", 3));
        m.put("OMR", new DefinicaoMoeda("OMR", 3));
        m.put("JOD", new DefinicaoMoeda("JOD", 3));

        TABELA_MOEDAS_ISO4217 = Collections.unmodifiableMap(m);
    }

    public final DecimalFixo quantia;
    public final DefinicaoMoeda moeda;

    private Monetario(DecimalFixo quantia, DefinicaoMoeda moeda) {
        this.quantia = quantia;
        this.moeda = moeda;
    }

    public static Monetario deTexto(String texto, String codigoMoeda) {
        DefinicaoMoeda definicao = TABELA_MOEDAS_ISO4217.get(codigoMoeda);
        if (definicao == null) {
            throw new ErroMonetario("[Erro Monetário] Código de moeda não reconhecido (ISO 4217): '" + codigoMoeda + "'.");
        }
        return new Monetario(DecimalFixo.deTexto(texto, definicao.casas()), definicao);
    }

    public static Monetario deInteiro(BigInteger valor, String codigoMoeda) {
        DefinicaoMoeda definicao = TABELA_MOEDAS_ISO4217.get(codigoMoeda);
        if (definicao == null) {
            throw new ErroMonetario("[Erro Monetário] Código de moeda não reconhecido (ISO 4217): '" + codigoMoeda + "'.");
        }
        return new Monetario(DecimalFixo.deInteiro(valor, definicao.casas()), definicao);
    }

    public static Monetario deInteiro(long valor, String codigoMoeda) {
        return deInteiro(BigInteger.valueOf(valor), codigoMoeda);
    }

    private void exigirMesmaMoeda(Monetario outro, String operacao) {
        if (!this.moeda.codigo().equals(outro.moeda.codigo())) {
            throw new ErroMonetario("[Erro Monetário] Impossível " + operacao + " " + this.moeda.codigo() + " com " + outro.moeda.codigo() + ". Converta explicitamente antes da operação.");
        }
    }

    public Monetario somar(Monetario outro) {
        exigirMesmaMoeda(outro, "somar");
        return new Monetario(this.quantia.somar(outro.quantia), this.moeda);
    }

    public Monetario subtrair(Monetario outro) {
        exigirMesmaMoeda(outro, "subtrair");
        return new Monetario(this.quantia.subtrair(outro.quantia), this.moeda);
    }

    /** Multiplicação por fator escalar (ex.: quantidade ou taxa adimensional). */
    public Monetario multiplicar(DecimalFixo fator, ModoArredondamento modo) {
        return new Monetario(this.quantia.multiplicar(fator, modo), this.moeda);
    }

    public Monetario multiplicar(DecimalFixo fator) {
        return multiplicar(fator, DecimalFixo.MODO_PADRAO);
    }

    /**
     * Divisão por divisor escalar; razão entre monetários é escalar.
     * Se divisor é Monetario (mesma moeda), retorna DecimalFixo razão.
     * Se divisor é DecimalFixo, retorna Monetario.
     */
    public Object dividir(Object divisor, ModoArredondamento modo) {
        if (divisor instanceof Monetario m) {
            exigirMesmaMoeda(m, "dividir");
            return this.quantia.dividir(m.quantia, modo);
        }
        if (divisor instanceof DecimalFixo d) {
            return new Monetario(this.quantia.dividir(d, modo), this.moeda);
        }
        throw new IllegalArgumentException("Divisor deve ser DecimalFixo ou Monetario");
    }

    public Object dividir(Object divisor) {
        return dividir(divisor, DecimalFixo.MODO_PADRAO);
    }

    /** Conveniência tipada para divisor DecimalFixo */
    public Monetario dividir(DecimalFixo divisor, ModoArredondamento modo) {
        return new Monetario(this.quantia.dividir(divisor, modo), this.moeda);
    }

    public Monetario dividir(DecimalFixo divisor) {
        return dividir(divisor, DecimalFixo.MODO_PADRAO);
    }

    /** Razão entre dois monetários de mesma moeda -> DecimalFixo */
    public DecimalFixo dividir(Monetario divisor, ModoArredondamento modo) {
        exigirMesmaMoeda(divisor, "dividir");
        return this.quantia.dividir(divisor.quantia, modo);
    }

    public DecimalFixo dividir(Monetario divisor) {
        return dividir(divisor, DecimalFixo.MODO_PADRAO);
    }

    public int comparar(Monetario outro) {
        exigirMesmaMoeda(outro, "comparar");
        return this.quantia.comparar(outro.quantia);
    }

    public Monetario negar() {
        return new Monetario(this.quantia.negar(), this.moeda);
    }

    public String formatar() {
        return this.quantia.formatar() + " " + this.moeda.codigo();
    }

    public DecimalFixo getQuantia() {
        return quantia;
    }

    public DefinicaoMoeda getMoeda() {
        return moeda;
    }

    @Override
    public String toString() {
        return formatar();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Monetario that)) return false;
        return this.moeda.codigo().equals(that.moeda.codigo()) && this.quantia.comparar(that.quantia) == 0;
    }

    @Override
    public int hashCode() {
        return quantia.hashCode() * 31 + moeda.codigo().hashCode();
    }
}
