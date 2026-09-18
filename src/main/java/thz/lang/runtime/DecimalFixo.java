package thz.lang.runtime;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Decimal de ponto fixo escalado com precisão arbitrária (BigInteger) e
 * escala paramétrica (P,S). Operações normalizam os operandos à maior
 * escala e preservam essa escala no resultado; produtos/divisões são
 * computados exatos e arredondados uma única vez conforme o modo.
 * Port exato de runtime.ts — sem uso de BigDecimal.
 */
public final class DecimalFixo {

    public final BigInteger valorEscalado;
    public final int escala;

    public static final ModoArredondamento MODO_PADRAO = ModoArredondamento.BANCARIO;

    /** Escala canônica do motor quando nenhuma é declarada. */
    public static final int ESCALA_PADRAO = 4;

    public static final DecimalFixo ZERO = deInteiro(0);

    private static final Pattern LITERAL_DECIMAL = Pattern.compile("^-?\\d*(\\.\\d*)?$");

    /** Construtor interno por escalado; prefira as fábricas deTexto/deInteiro. */
    public DecimalFixo(BigInteger valorEscalado, int escala) {
        if (escala < 0) {
            throw new ErroDecimal("[Erro Decimal] Escala deve ser inteiro não negativo.");
        }
        this.valorEscalado = valorEscalado;
        this.escala = escala;
    }

    public DecimalFixo(BigInteger valorEscalado) {
        this(valorEscalado, ESCALA_PADRAO);
    }

    // ---- fábricas ----

    public static DecimalFixo deTexto(String texto, int escala) {
        if (escala < 0) {
            throw new ErroDecimal("[Erro Decimal] Escala deve ser inteiro não negativo.");
        }
        String limpo = texto.trim();
        if (!LITERAL_DECIMAL.matcher(limpo).matches() || limpo.isEmpty() || limpo.equals("-") || limpo.equals(".")) {
            throw new ErroDecimal("[Erro Decimal] Literal decimal inválido: '" + texto + "'.");
        }
        boolean negativo = limpo.startsWith("-");
        String semSinal = negativo ? limpo.substring(1) : limpo;

        String parteInteira;
        String parteFracionaria;
        int dot = semSinal.indexOf('.');
        if (dot >= 0) {
            parteInteira = semSinal.substring(0, dot);
            parteFracionaria = semSinal.substring(dot + 1);
        } else {
            parteInteira = semSinal;
            parteFracionaria = "";
        }
        if (parteFracionaria.length() > escala) {
            throw new ErroDecimal("[Erro Decimal] Literal com mais casas decimais (" + parteFracionaria.length() + ") que a escala declarada (" + escala + "): " + texto);
        }
        if (parteInteira.isEmpty()) parteInteira = "0";
        String fracao = padEnd(parteFracionaria, escala, '0');
        BigInteger baseInteira = new BigInteger(parteInteira);
        BigInteger potencia = BigInteger.TEN.pow(escala);
        BigInteger fracaoVal = fracao.isEmpty() ? BigInteger.ZERO : new BigInteger(fracao);
        BigInteger magnitude = baseInteira.multiply(potencia).add(fracaoVal);
        if (negativo) magnitude = magnitude.negate();
        return new DecimalFixo(magnitude, escala);
    }

    public static DecimalFixo deTexto(String texto) {
        return deTexto(texto, ESCALA_PADRAO);
    }

    public static DecimalFixo deInteiro(BigInteger valor, int escala) {
        if (escala < 0) {
            throw new ErroDecimal("[Erro Decimal] Escala deve ser inteiro não negativo.");
        }
        return new DecimalFixo(valor.multiply(BigInteger.TEN.pow(escala)), escala);
    }

    public static DecimalFixo deInteiro(BigInteger valor) {
        return deInteiro(valor, ESCALA_PADRAO);
    }

    public static DecimalFixo deInteiro(long valor, int escala) {
        return deInteiro(BigInteger.valueOf(valor), escala);
    }

    public static DecimalFixo deInteiro(long valor) {
        return deInteiro(BigInteger.valueOf(valor), ESCALA_PADRAO);
    }

    // ---- helpers ----

    private static String padEnd(String s, int targetLength, char padChar) {
        if (s.length() >= targetLength) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < targetLength) sb.append(padChar);
        return sb.toString();
    }

    /** Escala combinada de dois operandos: a maior das duas. */
    private static int escalaComum(DecimalFixo a, DecimalFixo b) {
        return Math.max(a.escala, b.escala);
    }

    private static BigInteger rescalonar(BigInteger escalado, int deEscala, int paraEscala, ModoArredondamento modo) {
        if (paraEscala == deEscala) return escalado;
        if (paraEscala > deEscala) {
            return escalado.multiply(BigInteger.TEN.pow(paraEscala - deEscala));
        }
        BigInteger fator = BigInteger.TEN.pow(deEscala - paraEscala);
        boolean negativo = escalado.signum() < 0;
        BigInteger absoluto = negativo ? escalado.negate() : escalado;
        BigInteger quociente = absoluto.divide(fator);
        BigInteger resto = absoluto.remainder(fator);

        BigInteger arredondado = quociente;
        if (modo != ModoArredondamento.TRUNCAR && !resto.equals(BigInteger.ZERO)) {
            int cmp = resto.multiply(BigInteger.TWO).compareTo(fator);
            if (cmp > 0) {
                arredondado = quociente.add(BigInteger.ONE);
            } else if (cmp == 0 && modo == ModoArredondamento.BANCARIO) {
                // Empate: escolhe o vizinho de menor magnitude par (half-even).
                arredondado = quociente.mod(BigInteger.TWO).equals(BigInteger.ZERO) ? quociente : quociente.add(BigInteger.ONE);
            } else if (cmp == 0 && modo == ModoArredondamento.MEIA_CIMA) {
                arredondado = quociente.add(BigInteger.ONE);
            }
        }
        return negativo ? arredondado.negate() : arredondado;
    }

    private DecimalFixo normalizar(int escalaDestino, ModoArredondamento modo) {
        return new DecimalFixo(rescalonar(this.valorEscalado, this.escala, escalaDestino, modo), escalaDestino);
    }

    private DecimalFixo normalizar(int escalaDestino) {
        return normalizar(escalaDestino, MODO_PADRAO);
    }

    // ---- operações ----

    public DecimalFixo somar(DecimalFixo outro) {
        int escala = escalaComum(this, outro);
        BigInteger a = this.normalizar(escala).valorEscalado;
        BigInteger b = outro.normalizar(escala).valorEscalado;
        return new DecimalFixo(a.add(b), escala);
    }

    public DecimalFixo subtrair(DecimalFixo outro) {
        int escala = escalaComum(this, outro);
        BigInteger a = this.normalizar(escala).valorEscalado;
        BigInteger b = outro.normalizar(escala).valorEscalado;
        return new DecimalFixo(a.subtract(b), escala);
    }

    /**
     * Produto computado exato em BigInteger (escala s1+s2) e reescalado uma única
     * vez para a escala comum dos operandos, com arredondamento explícito.
     */
    public DecimalFixo multiplicar(DecimalFixo outro, ModoArredondamento modo) {
        BigInteger produtoExato = this.valorEscalado.multiply(outro.valorEscalado);
        int escalaExata = this.escala + outro.escala;
        int escalaAlvo = escalaComum(this, outro);
        return new DecimalFixo(rescalonar(produtoExato, escalaExata, escalaAlvo, modo), escalaAlvo);
    }

    public DecimalFixo multiplicar(DecimalFixo outro) {
        return multiplicar(outro, MODO_PADRAO);
    }

    /**
     * Divisão com dígitos de guarda: quociente exato é obtido em precisão
     * ampliada e arredondado uma única vez para a escala comum.
     */
    public DecimalFixo dividir(DecimalFixo outro, ModoArredondamento modo) {
        if (outro.valorEscalado.equals(BigInteger.ZERO)) {
            throw new ErroDecimal("[Erro Decimal] Divisão por zero.");
        }
        int escala = escalaComum(this, outro);
        BigInteger a = this.normalizar(escala).valorEscalado;
        BigInteger b = outro.normalizar(escala).valorEscalado;
        BigInteger numeradorAmpliado = a.multiply(BigInteger.TEN.pow(escala));
        boolean negativoResultado = (numeradorAmpliado.signum() < 0) != (b.signum() < 0);
        BigInteger num = negativoNumerico(numeradorAmpliado);
        BigInteger den = negativoNumerico(b);
        BigInteger quociente = num.divide(den);
        BigInteger resto = num.remainder(den);

        BigInteger escaladoFinal = quociente;
        if (modo != ModoArredondamento.TRUNCAR && !resto.equals(BigInteger.ZERO)) {
            int cmp = resto.multiply(BigInteger.TWO).compareTo(den);
            if (cmp > 0) escaladoFinal = escaladoFinal.add(BigInteger.ONE);
            else if (cmp == 0 && modo == ModoArredondamento.BANCARIO) escaladoFinal = quociente.mod(BigInteger.TWO).equals(BigInteger.ZERO) ? quociente : quociente.add(BigInteger.ONE);
            else if (cmp == 0 && modo == ModoArredondamento.MEIA_CIMA) escaladoFinal = escaladoFinal.add(BigInteger.ONE);
        }
        return new DecimalFixo(negativoResultado ? escaladoFinal.negate() : escaladoFinal, escala);
    }

    public DecimalFixo dividir(DecimalFixo outro) {
        return dividir(outro, MODO_PADRAO);
    }

    public DecimalFixo negar() {
        return new DecimalFixo(this.valorEscalado.negate(), this.escala);
    }

    public DecimalFixo abs() {
        return new DecimalFixo(this.valorEscalado.signum() < 0 ? this.valorEscalado.negate() : this.valorEscalado, this.escala);
    }

    /** Comparações normalizam à escala comum antes de comparar escalados. */
    public int comparar(DecimalFixo outro) {
        int escala = escalaComum(this, outro);
        BigInteger a = this.normalizar(escala).valorEscalado;
        BigInteger b = outro.normalizar(escala).valorEscalado;
        return a.compareTo(b) < 0 ? -1 : a.compareTo(b) > 0 ? 1 : 0;
    }

    public String formatar() {
        BigInteger divisor = BigInteger.TEN.pow(this.escala);
        boolean negativo = this.valorEscalado.signum() < 0;
        BigInteger absoluto = negativo ? this.valorEscalado.negate() : this.valorEscalado;
        BigInteger inteiro = absoluto.divide(divisor);
        if (this.escala == 0) return (negativo ? "-" : "") + inteiro.toString();
        BigInteger fracaoVal = absoluto.remainder(divisor);
        String fracao = padStart(fracaoVal.toString(), this.escala, '0');
        return (negativo ? "-" : "") + inteiro.toString() + "." + fracao;
    }

    /** Reescala mantendo o valor matemático (com arredondamento se necessário). */
    public DecimalFixo paraEscala(int escala, ModoArredondamento modo) {
        return this.normalizar(escala, modo);
    }

    public DecimalFixo paraEscala(int escala) {
        return paraEscala(escala, MODO_PADRAO);
    }

    private static BigInteger negativoNumerico(BigInteger v) {
        return v.signum() < 0 ? v.negate() : v;
    }

    private static String padStart(String s, int targetLength, char padChar) {
        if (s.length() >= targetLength) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < targetLength; i++) sb.append(padChar);
        sb.append(s);
        return sb.toString();
    }

    public BigInteger getValorEscalado() {
        return valorEscalado;
    }

    public int getEscala() {
        return escala;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecimalFixo d)) return false;
        // Comparação por valor matemático normalizado à escala comum
        return this.comparar(d) == 0;
    }

    @Override
    public int hashCode() {
        BigInteger canonico = valorEscalado;
        int escalaCanonica = escala;
        if (canonico.signum() == 0) {
            return 0;
        }
        while (escalaCanonica > 0 && canonico.remainder(BigInteger.TEN).signum() == 0) {
            canonico = canonico.divide(BigInteger.TEN);
            escalaCanonica--;
        }
        return canonico.hashCode() * 31 + escalaCanonica;
    }

    public java.math.BigDecimal paraBigDecimal() {
        return new java.math.BigDecimal(formatar());
    }

    @Override
    public String toString() {
        return formatar();
    }
}
