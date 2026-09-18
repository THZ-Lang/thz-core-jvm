package thz.lang.runtime;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DATA_HORA — tipo temporal gregoriano (BigInteger epoch segundos desde 1970-01-01T00:00:00).
 * Replica exatamente runtime.ts incluindo validações e algoritmos Hinnant.
 */
public final class DataHoraThz {

    public final BigInteger epochSegundos;

    private static final Pattern FORMATO_DATA_HORA = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2})(?::(\\d{2}))?$");
    private static final BigInteger SEGUNDOS_POR_DIA = BigInteger.valueOf(86400);
    private static final BigInteger SEGUNDOS_POR_HORA = BigInteger.valueOf(3600);
    private static final BigInteger SEGUNDOS_POR_MINUTO = BigInteger.valueOf(60);

    public DataHoraThz(BigInteger epochSegundos) {
        this.epochSegundos = epochSegundos;
    }

    // ---- fábricas ----

    public static DataHoraThz deComponentes(int ano, int mes, int dia, int hora, int minuto, int segundo) {
        if (hora < 0 || hora > 23) throw new ErroData("[Erro DataHora] Hora inválida: " + hora);
        if (minuto < 0 || minuto > 59) throw new ErroData("[Erro DataHora] Minuto inválido: " + minuto);
        if (segundo < 0 || segundo > 59) throw new ErroData("[Erro DataHora] Segundo inválido: " + segundo);
        // valida mês/dia via DataThz (lança [Erro Data] se inválido)
        DataThz.deComponentes(ano, mes, dia);
        BigInteger dias = DataThz.diasDesdeCivil(ano, mes, dia);
        BigInteger seg = dias.multiply(SEGUNDOS_POR_DIA)
                .add(BigInteger.valueOf(hora).multiply(SEGUNDOS_POR_HORA))
                .add(BigInteger.valueOf(minuto).multiply(SEGUNDOS_POR_MINUTO))
                .add(BigInteger.valueOf(segundo));
        return new DataHoraThz(seg);
    }

    public static DataHoraThz deComponentes(int ano, int mes, int dia, int hora, int minuto) {
        return deComponentes(ano, mes, dia, hora, minuto, 0);
    }

    public static DataHoraThz deTexto(String texto) {
        Matcher m = FORMATO_DATA_HORA.matcher(texto.trim());
        if (!m.matches()) throw new ErroData("[Erro DataHora] Formato de DATA_HORA inválido (esperado AAAA-MM-DDTHH:MM[:SS]): '" + texto + "'");
        int ano = Integer.parseInt(m.group(1), 10);
        int mes = Integer.parseInt(m.group(2), 10);
        int dia = Integer.parseInt(m.group(3), 10);
        int hora = Integer.parseInt(m.group(4), 10);
        int minuto = Integer.parseInt(m.group(5), 10);
        int segundo = m.group(6) != null ? Integer.parseInt(m.group(6), 10) : 0;
        return deComponentes(ano, mes, dia, hora, minuto, segundo);
    }

    // ---- helpers para dia/tempo ----

    private static BigInteger epochDiasDeSegundos(BigInteger epochSegundos) {
        if (epochSegundos.signum() >= 0) {
            return epochSegundos.divide(SEGUNDOS_POR_DIA);
        } else {
            // (epochSegundos - 86399) / 86400  — divisão truncada para negativo replicando floor
            return epochSegundos.subtract(BigInteger.valueOf(86399)).divide(SEGUNDOS_POR_DIA);
        }
    }

    private static BigInteger restoDiaPositivo(BigInteger epochSegundos) {
        // (epochSegundos % 86400 + 86400) % 86400  -> resto positivo 0..86399
        BigInteger r = epochSegundos.remainder(SEGUNDOS_POR_DIA);
        if (r.signum() < 0) r = r.add(SEGUNDOS_POR_DIA);
        return r;
    }

    // ---- getters ----

    public DataThz getData() {
        return new DataThz(epochDiasDeSegundos(this.epochSegundos));
    }

    public int getHora() {
        BigInteger resto = restoDiaPositivo(this.epochSegundos);
        return resto.divide(SEGUNDOS_POR_HORA).intValue();
    }

    public int getMinuto() {
        BigInteger resto = restoDiaPositivo(this.epochSegundos);
        return resto.remainder(SEGUNDOS_POR_HORA).divide(SEGUNDOS_POR_MINUTO).intValue();
    }

    public int getSegundo() {
        BigInteger resto = restoDiaPositivo(this.epochSegundos);
        return resto.remainder(SEGUNDOS_POR_MINUTO).intValue();
    }

    // aliases para compatibilidade TS (propriedades hora/minuto/segundo)
    public int hora() { return getHora(); }
    public int minuto() { return getMinuto(); }
    public int segundo() { return getSegundo(); }
    public DataThz data() { return getData(); }

    // ---- operações ----

    public DataHoraThz adicionarSegundos(BigInteger s) {
        return new DataHoraThz(this.epochSegundos.add(s));
    }

    public DataHoraThz adicionarSegundos(long s) {
        return adicionarSegundos(BigInteger.valueOf(s));
    }

    public DataHoraThz adicionarMinutos(BigInteger m) {
        return adicionarSegundos(m.multiply(SEGUNDOS_POR_MINUTO));
    }

    public DataHoraThz adicionarMinutos(long m) {
        return adicionarMinutos(BigInteger.valueOf(m));
    }

    public DataHoraThz adicionarHoras(BigInteger h) {
        return adicionarSegundos(h.multiply(SEGUNDOS_POR_HORA));
    }

    public DataHoraThz adicionarHoras(long h) {
        return adicionarHoras(BigInteger.valueOf(h));
    }

    public int comparar(DataHoraThz outro) {
        int c = this.epochSegundos.compareTo(outro.epochSegundos);
        return c == 0 ? 0 : c < 0 ? -1 : 1;
    }

    public String formatar() {
        BigInteger dias = epochDiasDeSegundos(this.epochSegundos);
        BigInteger resto = restoDiaPositivo(this.epochSegundos);
        int[] ymd = DataThz.civilDeDias(dias);
        int h = resto.divide(SEGUNDOS_POR_HORA).intValue();
        int m = resto.remainder(SEGUNDOS_POR_HORA).divide(SEGUNDOS_POR_MINUTO).intValue();
        int s = resto.remainder(SEGUNDOS_POR_MINUTO).intValue();
        String base = padStart(String.valueOf(ymd[0]), 4, '0') + "-" + padStart(String.valueOf(ymd[1]), 2, '0') + "-" + padStart(String.valueOf(ymd[2]), 2, '0') + "T" + padStart(String.valueOf(h), 2, '0') + ":" + padStart(String.valueOf(m), 2, '0');
        return s != 0 ? base + ":" + padStart(String.valueOf(s), 2, '0') : base;
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
        if (!(o instanceof DataHoraThz that)) return false;
        return this.epochSegundos.equals(that.epochSegundos);
    }

    @Override
    public int hashCode() {
        return epochSegundos.hashCode();
    }

    @Override
    public String toString() {
        return formatar();
    }
}
