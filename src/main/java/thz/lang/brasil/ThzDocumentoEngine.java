package thz.lang.brasil;

/**
 * Motor de Documentos Nacionais — CPF, CNPJ, Título de Eleitor, CNH, PIS e Telefone.
 */
public final class ThzDocumentoEngine {

    private ThzDocumentoEngine() {}

    public static String formatarCpf(String cpf) {
        if (cpf == null) return "";
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) return cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static String formatarCnpj(String cnpj) {
        if (cnpj == null) return "";
        String d = cnpj.replaceAll("\\D", "");
        if (d.length() != 14) return cnpj;
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    public static String formatarTelefone(String tel) {
        if (tel == null) return "";
        String d = tel.replaceAll("\\D", "");
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        } else if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return tel;
    }

    public static boolean validarTituloEleitor(String titulo) {
        if (titulo == null) return false;
        String d = titulo.replaceAll("\\D", "");
        if (d.length() != 12) return false;
        if (d.matches("(\\d)\\1{11}")) return false;

        int estado = Integer.parseInt(d.substring(8, 10));
        if (estado < 1 || estado > 28) return false;

        int soma1 = 0;
        for (int i = 0; i < 8; i++) {
            soma1 += (d.charAt(i) - '0') * (i + 2);
        }
        int resto1 = soma1 % 11;
        int dv1 = (resto1 == 10 || resto1 == 0) ? 0 : resto1;
        if (dv1 != (d.charAt(10) - '0')) return false;

        int soma2 = (d.charAt(8) - '0') * 7 + (d.charAt(9) - '0') * 8 + dv1 * 9;
        int resto2 = soma2 % 11;
        int dv2 = (resto2 == 10 || resto2 == 0) ? 0 : resto2;
        return dv2 == (d.charAt(11) - '0');
    }

    public static boolean validarCnh(String cnh) {
        if (cnh == null) return false;
        String d = cnh.replaceAll("\\D", "");
        if (d.length() != 11) return false;
        if (d.matches("(\\d)\\1{10}")) return false;

        int soma1 = 0;
        int peso1 = 9;
        for (int i = 0; i < 9; i++) {
            soma1 += (d.charAt(i) - '0') * peso1--;
        }
        int dv1 = soma1 % 11;
        if (dv1 >= 10) dv1 = 0;
        if (dv1 != (d.charAt(9) - '0')) return false;

        int soma2 = 0;
        int peso2 = 1;
        for (int i = 0; i < 9; i++) {
            soma2 += (d.charAt(i) - '0') * peso2++;
        }
        int dv2 = soma2 % 11;
        if (dv2 >= 10) dv2 = 0;
        return dv2 == (d.charAt(10) - '0');
    }

    public static boolean validarPis(String pis) {
        if (pis == null) return false;
        String d = pis.replaceAll("\\D", "");
        if (d.length() != 11) return false;
        if (d.matches("(\\d)\\1{10}")) return false;

        int[] pesos = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (d.charAt(i) - '0') * pesos[i];
        }
        int resto = 11 - (soma % 11);
        int dv = (resto == 10 || resto == 11) ? 0 : resto;
        return dv == (d.charAt(10) - '0');
    }
}
