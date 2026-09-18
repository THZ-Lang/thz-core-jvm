package thz.lang.runtime;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DATA — tipo temporal gregoriano (BigInteger epoch dias desde 1970-01-01).
 * Replica exatamente os algoritmos de Howard Hinnant (diasDesdeCivil/civilDeDias).
 */
public final class DataThz {

    public final BigInteger epochDias;

    private static final Pattern FORMATO_DATA = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

    public DataThz(BigInteger epochDias) {
        this.epochDias = epochDias;
    }

    // ---- helpers Hinnant ----

    private static boolean ehBissexto(int ano) {
        return (ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0;
    }

    private static int diasNoMes(int ano, int mes) {
        int[] tabela = {31, ehBissexto(ano) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        return tabela[mes - 1];
    }

    /** Howard Hinnant: dias desde civil 1970-01-01 — replica runtime.ts exatamente */
    static BigInteger diasDesdeCivil(int ano, int mes, int dia) {
        long y = ano;
        long m = mes;
        y -= m <= 2 ? 1 : 0;
        long era = Math.floorDiv(y, 400);
        long yoe = y - era * 400;
        long mp = m > 2 ? m - 3 : m + 9;
        long doy = (153 * mp + 2) / 5 + dia - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long dias = era * 146097 + doe - 719468;
        return BigInteger.valueOf(dias);
    }

    static int[] civilDeDias(BigInteger z) {
        // Replica TS: let zz = Number(z) + 719468; ... usando long para preservar exatidão até ~9e18
        // Para valores dentro do range de datas gregorianas, long é exato ( < 2^53).
        long zz = z.longValue() + 719468L;
        long era = Math.floorDiv(zz, 146097L);
        long doe = zz - era * 146097L;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        y += m <= 2 ? 1 : 0;
        return new int[]{(int) y, (int) m, (int) d};
    }

    // ---- fábricas ----

    public static DataThz deComponentes(int ano, int mes, int dia) {
        // Validação de inteiros: em Java todos são int, mas mantemos mensagem
        if (mes < 1 || mes > 12) throw new ErroData("[Erro Data] Mês inválido: " + mes);
        int dim = diasNoMes(ano, mes);
        if (dia < 1 || dia > dim) throw new ErroData("[Erro Data] Dia inválido: " + dia + " para " + ano + "-" + padStart(String.valueOf(mes), 2, '0'));
        return new DataThz(diasDesdeCivil(ano, mes, dia));
    }

    public static DataThz deTexto(String texto) {
        Matcher m = FORMATO_DATA.matcher(texto.trim());
        if (!m.matches()) throw new ErroData("[Erro Data] Formato de DATA inválido (esperado AAAA-MM-DD): '" + texto + "'");
        int ano = Integer.parseInt(m.group(1), 10);
        int mes = Integer.parseInt(m.group(2), 10);
        int dia = Integer.parseInt(m.group(3), 10);
        return deComponentes(ano, mes, dia);
    }

    // ---- getters ----

    public int[] getComponentesArray() {
        return civilDeDias(this.epochDias);
    }

    public int getAno() {
        return civilDeDias(this.epochDias)[0];
    }

    public int getMes() {
        return civilDeDias(this.epochDias)[1];
    }

    public int getDia() {
        return civilDeDias(this.epochDias)[2];
    }

    // Compatibilidade com TS: componentes getters
    public Componentes getComponentes() {
        int[] c = civilDeDias(this.epochDias);
        return new Componentes(c[0], c[1], c[2]);
    }

    public record Componentes(int ano, int mes, int dia) {}

    public int ano() { return getAno(); }
    public int mes() { return getMes(); }
    public int dia() { return getDia(); }

    // ---- operações ----

    public DataThz adicionarDias(BigInteger dias) {
        return new DataThz(this.epochDias.add(dias));
    }

    public DataThz adicionarDias(long dias) {
        return adicionarDias(BigInteger.valueOf(dias));
    }

    public DataThz adicionarDias(int dias) {
        return adicionarDias(BigInteger.valueOf(dias));
    }

    public BigInteger diferencaDias(DataThz outro) {
        return this.epochDias.subtract(outro.epochDias);
    }

    public int diaDaSemana() {
        // 1970-01-01 foi quinta-feira (4), domingo=0
        // Replica TS: Number(this.epochDias + 4n) %7 com ajuste negativo
        BigInteger v = this.epochDias.add(BigInteger.valueOf(4));
        // Use mod positivo (equivalente ao ajuste JS)
        int r = v.mod(BigInteger.valueOf(7)).intValue();
        return r;
    }

    public int comparar(DataThz outro) {
        int c = this.epochDias.compareTo(outro.epochDias);
        return c == 0 ? 0 : c < 0 ? -1 : 1;
    }

    public String formatar() {
        int[] c = civilDeDias(this.epochDias);
        return padStart(String.valueOf(c[0]), 4, '0') + "-" + padStart(String.valueOf(c[1]), 2, '0') + "-" + padStart(String.valueOf(c[2]), 2, '0');
    }

    private static String padStart(String s, int targetLength, char padChar) {
        if (s.length() >= targetLength) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < targetLength; i++) sb.append(padChar);
        sb.append(s);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataThz that)) return false;
        return this.epochDias.equals(that.epochDias);
    }

    @Override
    public int hashCode() {
        return epochDias.hashCode();
    }

    @Override
    public String toString() {
        return formatar();
    }
}
