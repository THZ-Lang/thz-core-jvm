package thz.lang.brasil;

import thz.lang.interpretador.ValorThz;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Motor de CEPs, endereços e UF do Brasil.
 */
public final class ThzCepEngine {

    private static final Set<String> UFS_VALIDAS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private static final Map<String, String> REGIOES_UF = Map.ofEntries(
            Map.entry("SP", "Sudeste"), Map.entry("RJ", "Sudeste"), Map.entry("MG", "Sudeste"), Map.entry("ES", "Sudeste"),
            Map.entry("PR", "Sul"), Map.entry("SC", "Sul"), Map.entry("RS", "Sul"),
            Map.entry("DF", "Centro-Oeste"), Map.entry("GO", "Centro-Oeste"), Map.entry("MT", "Centro-Oeste"), Map.entry("MS", "Centro-Oeste"),
            Map.entry("BA", "Nordeste"), Map.entry("PE", "Nordeste"), Map.entry("CE", "Nordeste"), Map.entry("MA", "Nordeste"),
            Map.entry("PB", "Nordeste"), Map.entry("RN", "Nordeste"), Map.entry("AL", "Nordeste"), Map.entry("SE", "Nordeste"), Map.entry("PI", "Nordeste"),
            Map.entry("AM", "Norte"), Map.entry("PA", "Norte"), Map.entry("RO", "Norte"), Map.entry("TO", "Norte"),
            Map.entry("AC", "Norte"), Map.entry("AP", "Norte"), Map.entry("RR", "Norte")
    );

    private ThzCepEngine() {}

    public static String formatarCep(String cep) {
        if (cep == null) return "";
        String digitos = cep.replaceAll("\\D", "");
        if (digitos.length() != 8) return cep;
        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }

    public static boolean validarUf(String uf) {
        if (uf == null) return false;
        return UFS_VALIDAS.contains(uf.trim().toUpperCase());
    }

    public static String regiaoUf(String uf) {
        if (uf == null) return "";
        return REGIOES_UF.getOrDefault(uf.trim().toUpperCase(), "Desconhecida");
    }

    public static ValorThz.Registro consultarCep(String cep) {
        Map<String, String> dados = ThzInternalDatabase.consultarCep(cep);
        Map<String, ValorThz> campos = new LinkedHashMap<>();
        campos.put("cep", ValorThz.TEXTO(dados.getOrDefault("cep", formatarCep(cep))));
        campos.put("logradouro", ValorThz.TEXTO(dados.getOrDefault("logradouro", "")));
        campos.put("bairro", ValorThz.TEXTO(dados.getOrDefault("bairro", "")));
        campos.put("cidade", ValorThz.TEXTO(dados.getOrDefault("cidade", "")));
        campos.put("uf", ValorThz.TEXTO(dados.getOrDefault("uf", "")));
        campos.put("ibge", ValorThz.TEXTO(dados.getOrDefault("ibge", "")));
        campos.put("ddd", ValorThz.TEXTO(dados.getOrDefault("ddd", "")));
        campos.put("regiao", ValorThz.TEXTO(regiaoUf(dados.getOrDefault("uf", ""))));
        return new ValorThz.Registro("EnderecoCep", campos);
    }

    public static String formatarEndereco(String logradouro, String numero, String complemento, String bairro, String cidade, String uf, String cep) {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null && !logradouro.isBlank()) sb.append(logradouro);
        if (numero != null && !numero.isBlank()) sb.append(", ").append(numero);
        if (complemento != null && !complemento.isBlank()) sb.append(" - ").append(complemento);
        if (bairro != null && !bairro.isBlank()) sb.append(", ").append(bairro);
        if (cidade != null && !cidade.isBlank()) sb.append(" - ").append(cidade);
        if (uf != null && !uf.isBlank()) sb.append("/").append(uf.toUpperCase());
        if (cep != null && !cep.isBlank()) sb.append(", CEP: ").append(formatarCep(cep));
        return sb.toString();
    }
}
