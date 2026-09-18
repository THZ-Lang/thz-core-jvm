package thz.lang.brasil;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Motor de Calendário Bancário e Feriados Nacionais do Brasil.
 */
public final class ThzFeriadoEngine {

    private ThzFeriadoEngine() {}

    public static boolean ehFeriadoNacional(LocalDate data) {
        if (data == null) return false;
        int ano = data.getYear();
        int mes = data.getMonthValue();
        int dia = data.getDayOfMonth();

        if (mes == 1 && dia == 1) return true;
        if (mes == 4 && dia == 21) return true;
        if (mes == 5 && dia == 1) return true;
        if (mes == 9 && dia == 7) return true;
        if (mes == 10 && dia == 12) return true;
        if (mes == 11 && dia == 2) return true;
        if (mes == 11 && dia == 15) return true;
        if (mes == 11 && dia == 20) return true;
        if (mes == 12 && dia == 25) return true;

        LocalDate pascoa = calcularDomingoPascoa(ano);
        LocalDate carnavalSegunda = pascoa.minusDays(48);
        LocalDate carnavalTerca = pascoa.minusDays(47);
        LocalDate sextaFeiraSanta = pascoa.minusDays(2);
        LocalDate corpusChristi = pascoa.plusDays(60);

        return data.equals(carnavalSegunda) || data.equals(carnavalTerca) || data.equals(sextaFeiraSanta) || data.equals(corpusChristi);
    }

    public static boolean ehDiaUtil(LocalDate data) {
        if (data == null) return false;
        DayOfWeek dow = data.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return !ehFeriadoNacional(data);
    }

    public static LocalDate proximoDiaUtil(LocalDate data) {
        if (data == null) return LocalDate.now();
        LocalDate cur = data;
        while (!ehDiaUtil(cur)) {
            cur = cur.plusDays(1);
        }
        return cur;
    }

    static LocalDate calcularDomingoPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
