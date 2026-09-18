package thz.lang.semantico;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Assinaturas estáticas de tipos e aridade para as funções da biblioteca padrão da THZ-LANG v3.0.
 */
public final class AssinaturasStdlib {

    public record Assinatura(int paramMin, int paramMax, Function<List<TipoThz>, TipoThz> retornoFn) {
        public Assinatura(int paramMin, int paramMax, TipoThz retornoFixo) {
            this(paramMin, paramMax, args -> retornoFixo);
        }
    }

    private static final TipoThz TIPO_LOGICO = Tipos.TIPOS_PRIMITIVOS.get("LOGICO");
    private static final TipoThz TIPO_TEXTO = Tipos.TIPOS_PRIMITIVOS.get("TEXTO");
    private static final TipoThz TIPO_INTEIRO_GENERICO = new TipoThz("INTEIRO64", CategoriaTipo.INTEIRO);
    private static final TipoThz TIPO_DECIMAL_GENERICO = new TipoThz("DECIMAL", CategoriaTipo.DECIMAL, 4, null, null, null);
    private static final TipoThz TIPO_DATA = Tipos.TIPOS_PRIMITIVOS.get("DATA");
    private static final TipoThz TIPO_DATA_HORA = Tipos.TIPOS_PRIMITIVOS.get("DATA_HORA");

    private static final Map<String, Assinatura> ASSINATURAS = criarAssinaturas();

    private AssinaturasStdlib() {}

    public static boolean ehStdlib(String nome) {
        return nome != null && ASSINATURAS.containsKey(nome);
    }

    public static Assinatura obter(String nome) {
        return ASSINATURAS.get(nome);
    }

    private static Map<String, Assinatura> criarAssinaturas() {
        Map<String, Assinatura> m = new LinkedHashMap<>();

        // ---- TEXTO ----
        m.put("TEXTO.comprimento", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("TEXTO.maiusculas", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.minusculas", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.aparar", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.contem", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("TEXTO.subtexto", new Assinatura(2, 3, TIPO_TEXTO));
        m.put("TEXTO.substituir", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("TEXTO.dividir", new Assinatura(2, 2, new TipoThz("FATIA[TEXTO]", CategoriaTipo.FATIA, null, null, "TEXTO", null)));
        m.put("TEXTO.juntar", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("TEXTO.deDecimal", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.deInteiro", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.deLogico", new Assinatura(1, 1, TIPO_TEXTO));

        // ---- MATEMATICA ----
        m.put("MATEMATICA.abs", new Assinatura(1, 1, (Function<List<TipoThz>, TipoThz>) args -> Tipos.ehInteiro(args.get(0)) ? TIPO_INTEIRO_GENERICO : args.get(0)));
        m.put("MATEMATICA.min", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.max", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.potencia", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.raiz", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.arredondar", new Assinatura(2, 2, (Function<List<TipoThz>, TipoThz>) args -> args.get(0)));
        m.put("MATEMATICA.aleatorio", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));

        // ---- DATA ----
        m.put("DATA.hoje", new Assinatura(0, 0, TIPO_DATA));
        m.put("DATA.agora", new Assinatura(0, 0, TIPO_DATA_HORA));
        m.put("DATA.criar", new Assinatura(3, 3, TIPO_DATA));
        m.put("DATA.criarDataHora", new Assinatura(5, 6, TIPO_DATA_HORA));
        m.put("DATA.adicionarDias", new Assinatura(2, 2, TIPO_DATA));
        m.put("DATA.adicionarHoras", new Assinatura(2, 2, TIPO_DATA_HORA));
        m.put("DATA.diferencaDias", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("DATA.ano", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.mes", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.dia", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.diaDaSemana", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.texto", new Assinatura(1, 1, TIPO_TEXTO));

        // ---- TELA ----
        m.put("TELA.renderizarFormulario", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("TELA.alerta", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("TELA.confirmar", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("TELA.pedirTexto", new Assinatura(2, 2, TIPO_TEXTO));

        // ---- DOCUMENTO ----
        m.put("DOCUMENTO.exportar", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarPdf", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarXlsx", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarDocx", new Assinatura(3, 3, TIPO_TEXTO));

        // ---- VERSAO ----
        m.put("VERSAO.obter", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("VERSAO.satisfaz", new Assinatura(2, 2, TIPO_LOGICO));

        // ---- INVENTÁRIO AUDITÁVEL (retornos estruturados de integração) ----
        TipoThz tipoRegistro = new TipoThz("REGISTRO", CategoriaTipo.REGISTRO);
        m.put("INVENTARIO.importar", new Assinatura(3, 3, tipoRegistro));
        m.put("INVENTARIO.validar", new Assinatura(3, 3, tipoRegistro));
        m.put("INVENTARIO.atualizar", new Assinatura(6, 6, tipoRegistro));
        m.put("INVENTARIO.consultar", new Assinatura(1, 1, tipoRegistro));
        m.put("INVENTARIO.historico", new Assinatura(3, 3,
                new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));

        // ---- ARQUIVO & DIRETORIO ----
        m.put("ARQUIVO.localizar", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("ARQUIVO.lerTexto", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("ARQUIVO.escreverTexto", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("ARQUIVO.anexarTexto", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("ARQUIVO.existe", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("ARQUIVO.remover", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("DIRETORIO.listar", new Assinatura(1, 1, new TipoThz("FATIA[TEXTO]", CategoriaTipo.FATIA, null, null, "TEXTO", null)));
        m.put("DIRETORIO.criar", new Assinatura(1, 1, TIPO_LOGICO));

        // ---- CONFIG ----
        m.put("CONFIG.obter", new Assinatura(1, 2, TIPO_TEXTO));
        m.put("CONFIG.carregarEnv", new Assinatura(0, 1, TIPO_LOGICO));
        m.put("CONFIG.projeto.nome", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("CONFIG.projeto.versao", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("CONFIG.projeto.autor", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("CONFIG.projeto.dialeto", new Assinatura(0, 0, TIPO_TEXTO));

        // ---- SEGURANCA ----
        m.put("SEGURANCA.sha256", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("SEGURANCA.sha512", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("SEGURANCA.hmacSha256", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.criptografarAes", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.descriptografarAes", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.hashSenha", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("SEGURANCA.verificarSenha", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("SEGURANCA.argon2", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("SEGURANCA.verificarArgon2", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("SEGURANCA.chacha20", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.descriptografarChaCha20", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.cofreSalvar", new Assinatura(3, 3, TIPO_LOGICO));
        m.put("SEGURANCA.cofreLer", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("SEGURANCA.gerarToken", new Assinatura(0, 1, TIPO_TEXTO));
        m.put("SEGURANCA.uuid", new Assinatura(0, 0, TIPO_TEXTO));

        // ---- VETOR ----
        m.put("VETOR.criar", new Assinatura(0, 1, TIPO_TEXTO));
        m.put("VETOR.similaridadeCosseno", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("VETOR.distanciaEuclidiana", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("VETOR.produtoEscalar", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("VETOR.normalizar", new Assinatura(1, 1, TIPO_TEXTO));

        // ---- IA & ML ----
        m.put("IA.embedding", new Assinatura(1, 2, TIPO_TEXTO));
        m.put("IA.similaridade", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("ML.classificar", new Assinatura(3, 3, TIPO_DECIMAL_GENERICO));
        m.put("ML.predizer", new Assinatura(3, 3, TIPO_DECIMAL_GENERICO));

        // ---- MENSAGERIA ----
        m.put("MENSAGERIA.publicar", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MENSAGERIA.consumir", new Assinatura(1, 2, TIPO_TEXTO));
        m.put("MENSAGERIA.tamanhoFila", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("MENSAGERIA.limparTopico", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("MENSAGERIA.driverAtivo", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("MENSAGERIA.statusConexao", new Assinatura(0, 0, TIPO_LOGICO));
        m.put("MENSAGERIA.urlAtiva", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("MENSAGERIA.conectar", new Assinatura(1, 2, TIPO_LOGICO));

        // ---- LOG ----
        m.put("LOG.info", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("LOG.aviso", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("LOG.erro", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("LOG.auditoria", new Assinatura(3, 3, TIPO_LOGICO));

        // ---- BANCO ----
        m.put("BANCO.conectar", new Assinatura(1, 4, TIPO_LOGICO));
        m.put("BANCO.executar", new Assinatura(1, 2, TIPO_INTEIRO_GENERICO));
        m.put("BANCO.executarEm", new Assinatura(2, 3, TIPO_INTEIRO_GENERICO));
        m.put("BANCO.consultar", new Assinatura(1, 2, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("BANCO.consultarEm", new Assinatura(2, 3, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("BANCO.consultarValor", new Assinatura(1, 2, TIPO_TEXTO));
        m.put("BANCO.iniciarTransacao", new Assinatura(0, 1, TIPO_LOGICO));
        m.put("BANCO.confirmarTransacao", new Assinatura(0, 1, TIPO_LOGICO));
        m.put("BANCO.cancelarTransacao", new Assinatura(0, 1, TIPO_LOGICO));
        m.put("BANCO.executarScript", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BANCO.driverAtivo", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("BANCO.salvar", new Assinatura(2, 2, (Function<List<TipoThz>, TipoThz>) args -> args.size() > 1 ? args.get(1) : TIPO_TEXTO));
        m.put("BANCO.buscarPorId", new Assinatura(2, 2, new TipoThz("REGISTRO", CategoriaTipo.REGISTRO, null, null, "REGISTRO", null)));
        m.put("BANCO.removerPorId", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("BANCO.criarTabela", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("BANCO.consultarVetorial", new Assinatura(3, 4, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("BANCO.fechar", new Assinatura(0, 1, TIPO_LOGICO));

        // ---- WEBVIEW ----
        m.put("WEBVIEW.iniciar", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("WEBVIEW.emitir", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("WEBVIEW.parar", new Assinatura(0, 0, TIPO_LOGICO));

        // ---- UI ----
        m.put("UI.temaPadrao", new Assinatura(0, 0, TIPO_TEXTO));
        m.put("UI.renderizarHtml", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("UI.gerarCodigo", new Assinatura(1, 1, TIPO_TEXTO));

        // ---------------- ESTATISTICA ----------------
        m.put("ESTATISTICA.media", new Assinatura(1, 1, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.mediana", new Assinatura(1, 1, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.moda", new Assinatura(1, 1, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.desvioPadrao", new Assinatura(1, 2, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.variancia", new Assinatura(1, 2, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.correlacao", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.percentil", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.zScore", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("ESTATISTICA.outliers", new Assinatura(1, 1, new TipoThz("FATIA[DECIMAL]", CategoriaTipo.FATIA, null, null, "DECIMAL", null)));
        m.put("ESTATISTICA.regressao", new Assinatura(2, 2, new TipoThz("REGISTRO", CategoriaTipo.REGISTRO, null, null, "REGISTRO", null)));

        // ---------------- DAX / BI / METRICAS ANALITICAS ----------------
        m.put("DAX.acumuladoAno", new Assinatura(4, 4, TIPO_DECIMAL_GENERICO));
        m.put("DAX.variacaoPeriodo", new Assinatura(2, 2, TIPO_DECIMAL_GENERICO));
        m.put("DAX.contagemDistintos", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("DAX.ranking", new Assinatura(2, 3, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("DAX.percentualTotal", new Assinatura(2, 2, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("DAX.kpi", new Assinatura(3, 4, new TipoThz("REGISTRO", CategoriaTipo.REGISTRO, null, null, "REGISTRO", null)));

        // ---------------- PLANILHA / CSV / TABELAS ----------------
        m.put("PLANILHA.lerCsv", new Assinatura(1, 2, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("PLANILHA.escreverCsv", new Assinatura(2, 3, TIPO_LOGICO));
        m.put("PLANILHA.procv", new Assinatura(4, 4, (Function<List<TipoThz>, TipoThz>) args -> TIPO_TEXTO));
        m.put("PLANILHA.pivotar", new Assinatura(4, 5, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));

        // ---------------- DADOS & DATA QUALITY ----------------
        m.put("DADOS.sanitizar", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("DADOS.decimalPtBr", new Assinatura(1, 1, TIPO_DECIMAL_GENERICO));
        m.put("DADOS.dataPtBr", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("DADOS.validarCpf", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("DADOS.validarCnpj", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("DADOS.mascarar", new Assinatura(1, 3, TIPO_TEXTO));
        m.put("DADOS.removerDuplicatas", new Assinatura(1, 2, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));
        m.put("DADOS.imputarNulos", new Assinatura(3, 3, new TipoThz("FATIA[REGISTRO]", CategoriaTipo.FATIA, null, null, "REGISTRO", null)));

        // ---------------- BRASIL (CEP, PIX, Boleto, Documentos, Feriados) ----------------
        m.put("BRASIL.cep", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.consultarCep", new Assinatura(1, 1, new TipoThz("REGISTRO", CategoriaTipo.REGISTRO, null, null, "REGISTRO", null)));
        m.put("BRASIL.cadastrarCep", new Assinatura(7, 7, TIPO_LOGICO));
        m.put("BRASIL.validarUf", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.regiaoUf", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.formatarEndereco", new Assinatura(7, 7, TIPO_TEXTO));
        m.put("BRASIL.pixCopiaECola", new Assinatura(5, 5, TIPO_TEXTO));
        m.put("BRASIL.validarChavePix", new Assinatura(1, 2, TIPO_LOGICO));
        m.put("BRASIL.validarLinhaDigitavel", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.linhaParaCodigoBarras", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.valorBoleto", new Assinatura(1, 1, TIPO_DECIMAL_GENERICO));
        m.put("BRASIL.formatarCpf", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.formatarCnpj", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.formatarTelefone", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.validarTituloEleitor", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.validarCnh", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.validarPis", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.ehFeriadoNacional", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.ehDiaUtil", new Assinatura(1, 1, TIPO_LOGICO));
        m.put("BRASIL.proximoDiaUtil", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("BRASIL.valorPorExtenso", new Assinatura(1, 1, TIPO_TEXTO));

        // ---------------- SNAPSHOT & COMPACTACAO CACHE ----------------
        m.put("SNAPSHOT.criar", new Assinatura(0, 2, TIPO_TEXTO));
        m.put("SNAPSHOT.restaurar", new Assinatura(0, 2, TIPO_LOGICO));
        m.put("SNAPSHOT.tamanho", new Assinatura(0, 0, TIPO_INTEIRO_GENERICO));
        m.put("SNAPSHOT.limpar", new Assinatura(0, 0, TIPO_LOGICO));
        m.put("SNAPSHOT.verificar", new Assinatura(0, 1, TIPO_LOGICO));

        // ---- NATIVO ----
        m.put("NATIVO.somar_rapido", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("NATIVO.calcular_hash_customizado", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("NATIVO.versao_rust", new Assinatura(0, 0, TIPO_TEXTO));

        return Collections.unmodifiableMap(m);
    }
}
