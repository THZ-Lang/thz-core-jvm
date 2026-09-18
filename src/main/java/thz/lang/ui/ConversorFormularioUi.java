package thz.lang.ui;

import thz.lang.interpretador.ValorThz;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte um ValorThz.Registro (ESTRUTURA THZ) em árvore declarativa ThzUiComponente.
 *
 * Mapeia tipos THZ para widgets HTML5 via ThzUiMaker:
 *   TEXTO senha/cor/arquivo/long => CAMPO_TEXTO com variantes
 *   INTEIRO slider/spinner        => CAMPO_NUMERO com metadata
 *   DECIMAL/MONETARIO             => CAMPO_MOEDA
 *   LOGICO switch                 => INTERRUPTOR
 *   ENUMERADO radio               => RADIO ou SELECAO
 *   FATIA[TEXTO|REGISTRO]         => TABELA_DADOS / lista
 *   Outros                        => CAMPO_TEXTO genérico
 *
 * Heurística de nome de campo compatível com FabricaCamposFormulario do thz-gui.
 */
public final class ConversorFormularioUi {

    private ConversorFormularioUi() {}

    public static ThzUiComponente converter(ValorThz.Registro registro, String operacaoAlvo) {
        String titulo = extrairTitulo(registro);
        String nomeOp = extrairNomeSimples(operacaoAlvo);

        ThzUiMaker raiz = ThzUiMaker.container("thz_form_raiz", null);
        // Header card
        ThzUiMaker header = ThzUiMaker.card("thz_header_" + slug(registro.nomeEstrutura()), titulo, h -> {
            h.adicionar(ThzUiMaker.alerta("thz_alerta_header", "info",
                    "Estrutura: " + registro.nomeEstrutura() + "  |  Operação: " + operacaoAlvo));
        });

        ThzUiMaker formCard = ThzUiMaker.card("thz_form_card", "Formulário — " + registro.nomeEstrutura(), null);

        for (var entry : registro.campos().entrySet()) {
            String nomeCampo = entry.getKey();
            if ("titulo".equalsIgnoreCase(nomeCampo)) continue;
            ValorThz valor = entry.getValue();
            ThzUiMaker campo = criarComponenteCampo(nomeCampo, valor);
            formCard.adicionar(campo);
        }

        // Footer ações
        ThzUiMaker linhaAcoes = ThzUiMaker.linha("thz_acoes", a -> {
            a.adicionar(ThzUiMaker.botao("thz_btn_acao", nomeOp, operacaoAlvo != null ? operacaoAlvo : "acao"));
            a.adicionar(ThzUiMaker.botao("thz_btn_limpar", "Restaurar", "__thz_restaurar__").comPropriedade("variante", "secundario"));
        });
        formCard.adicionar(ThzUiMaker.divisor());
        formCard.adicionar(linhaAcoes);
        formCard.adicionar(ThzUiMaker.alerta("thz_status", "info", "Preencha os campos e clique em '" + nomeOp + "' para submeter."));

        raiz.adicionar(header);
        raiz.adicionar(formCard);
        return raiz.construir();
    }

    public static String converterParaHtml(ValorThz.Registro registro, String operacaoAlvo, ThzUiTema tema) {
        ThzUiComponente raiz = converter(registro, operacaoAlvo);
        String titulo = extrairTitulo(registro);
        return ThzUiHtmlEmitter.renderizarPaginaCompleta(titulo, raiz, tema != null ? tema : ThzUiTema.escuroGlass());
    }

    private static ThzUiMaker criarComponenteCampo(String nomeCampo, ValorThz valor) {
        String rotulo = formatarRotulo(nomeCampo);
        String id = "thz_campo_" + slug(nomeCampo);

        if (valor instanceof ValorThz.Texto t) {
            if (ehCampoSenha(nomeCampo)) {
                return ThzUiMaker.campoTexto(id, rotulo + " (Senha)", "••••••••", nomeCampo)
                        .comPropriedade("valor", t.valor())
                        .comPropriedade("tipoHtml", "password");
            }
            if (ehCampoCor(nomeCampo)) {
                return ThzUiMaker.campoTexto(id, rotulo + " (Cor)", "#RRGGBB", nomeCampo)
                        .comPropriedade("valor", t.valor())
                        .comPropriedade("tipoHtml", "color");
            }
            if (ehCampoArquivo(nomeCampo)) {
                return ThzUiMaker.campoTexto(id, rotulo + " (Arquivo)", "Selecione um arquivo", nomeCampo)
                        .comPropriedade("valor", t.valor())
                        .comPropriedade("tipoHtml", "file");
            }
            if (ehCampoTextoLongo(nomeCampo)) {
                return ThzUiMaker.campoTexto(id, rotulo, "Descrição detalhada...", nomeCampo)
                        .comPropriedade("valor", t.valor())
                        .comPropriedade("multilinha", "true");
            }
            return ThzUiMaker.campoTexto(id, rotulo, "", nomeCampo)
                    .comPropriedade("valor", t.valor());
        }

        if (valor instanceof ValorThz.Inteiro i) {
            if (ehCampoSlider(nomeCampo)) {
                return ThzUiMaker.campoNumero(id, rotulo + " (Slider 0-100)", nomeCampo)
                        .comPropriedade("valor", i.valor().intValue())
                        .comPropriedade("min", 0).comPropriedade("max", 100)
                        .comPropriedade("tipoHtml", "range");
            }
            if (ehCampoSpinner(nomeCampo)) {
                return ThzUiMaker.campoNumero(id, rotulo, nomeCampo)
                        .comPropriedade("valor", i.valor().intValue())
                        .comPropriedade("passo", 1);
            }
            return ThzUiMaker.campoNumero(id, rotulo, nomeCampo)
                    .comPropriedade("valor", i.valor().intValue());
        }

        if (valor instanceof ValorThz.Decimal d) {
            return ThzUiMaker.campoMoeda(id, rotulo, "BRL", nomeCampo)
                    .comPropriedade("valor", d.valor().formatar());
        }

        if (valor instanceof ValorThz.Monetario m) {
            return ThzUiMaker.campoMoeda(id, rotulo, m.valor().moeda.codigo(), nomeCampo)
                    .comPropriedade("valor", m.valor().formatar());
        }

        if (valor instanceof ValorThz.Logico l) {
            if (ehCampoSwitch(nomeCampo)) {
                return ThzUiMaker.interruptor(id, rotulo, nomeCampo)
                        .comPropriedade("valor", String.valueOf(l.valor()));
            }
            // checkbox mapeado para interruptor (compatível HTML)
            return ThzUiMaker.interruptor(id, rotulo + " (Ativo / Sim)", nomeCampo)
                    .comPropriedade("valor", String.valueOf(l.valor()));
        }

        if (valor instanceof ValorThz.Enumerado en) {
            List<String> opcoes = List.of(en.valor());
            // Tenta inferir opções extras pelo nome do campo — fallback para valor único
            return ThzUiMaker.selecao(id, rotulo, opcoes, nomeCampo);
        }

        if (valor instanceof ValorThz.Fatia fatia) {
            if (isFatiaDeRegistro(fatia)) {
                // Fase 2: renderiza como tabela editável (TABELA_DADOS) com dados reais
                List<ThzUiComponente> linhas = new ArrayList<>();
                // header
                if (!fatia.elementos().isEmpty() && fatia.elementos().get(0) instanceof ValorThz.Registro r0) {
                    String header = String.join(" | ", r0.campos().keySet());
                    linhas.add(ThzUiMaker.alerta(id + "_hdr", "info", rotulo + " — " + fatia.elementos().size() + " registro(s): " + header).construir());
                }
                // amostra de até 5 linhas como alerta detalhado (evita HTML table complexo no MVP)
                int max = Math.min(fatia.elementos().size(), 5);
                for (int i = 0; i < max; i++) {
                    ValorThz el = fatia.elementos().get(i);
                    linhas.add(ThzUiMaker.alerta(id + "_row_" + i, "info", "[" + i + "] " + (el != null ? el.formatar() : "NULO")).construir());
                }
                if (fatia.elementos().size() > max) {
                    linhas.add(ThzUiMaker.alerta(id + "_more", "info", "... e mais " + (fatia.elementos().size() - max) + " registro(s)").construir());
                }
                ThzUiMaker tabela = ThzUiMaker.novo(id, ThzUiComponente.TipoUi.TABELA_DADOS);
                tabela.comPropriedade("rotulo", rotulo);
                tabela.comPropriedade("total", fatia.elementos().size());
                for (var l : linhas) tabela.adicionar(l);
                return tabela;
            }
            List<String> elems = new ArrayList<>();
            for (ValorThz e : fatia.elementos()) elems.add(e != null ? e.formatar() : "NULO");
            // Fase 2: fatia simples vira lista editável como string CSV + campo texto
            if (elems.size() <= 8) {
                return ThzUiMaker.campoTexto(id, rotulo + " (lista, separe por ;)", String.join("; ", elems), nomeCampo)
                        .comPropriedade("valor", String.join("; ", elems));
            }
            return ThzUiMaker.alerta(id, "info", rotulo + ": [" + String.join(", ", elems.subList(0, Math.min(8, elems.size()))) + (elems.size() > 8 ? " ..." : "") + "] (" + elems.size() + " itens)");
        }

        if (valor instanceof ValorThz.Data || valor instanceof ValorThz.DataHora) {
            return ThzUiMaker.campoData(id, rotulo, nomeCampo)
                    .comPropriedade("valor", valor.formatar());
        }

        if (valor instanceof ValorThz.Nulo) {
            return ThzUiMaker.campoTexto(id, rotulo, "", nomeCampo)
                    .comPropriedade("valor", "");
        }

        // Fallback genérico
        return ThzUiMaker.campoTexto(id, rotulo, "", nomeCampo)
                .comPropriedade("valor", valor != null ? valor.formatar() : "");
    }

    public static String extrairTitulo(ValorThz.Registro reg) {
        if (reg != null && reg.campos().containsKey("titulo")) {
            ValorThz t = reg.campos().get("titulo");
            if (t instanceof ValorThz.Texto txt && !txt.valor().isBlank()) return txt.valor();
        }
        return reg != null ? "Formulário THZ — " + reg.nomeEstrutura() : "Formulário THZ";
    }

    private static String extrairNomeSimples(String alvo) {
        if (alvo == null || alvo.isBlank()) return "Salvar";
        int ponto = alvo.lastIndexOf('.');
        return ponto >= 0 ? alvo.substring(ponto + 1) : alvo;
    }

    private static boolean isFatiaDeRegistro(ValorThz.Fatia f) {
        return f != null && !f.elementos().isEmpty() && f.elementos().get(0) instanceof ValorThz.Registro;
    }

    // ---- Heurísticas compatíveis com FabricaCamposFormulario ----
    private static boolean ehCampoSenha(String n) { String s=n.toLowerCase(); return s.contains("senha")||s.contains("password")||s.contains("pwd"); }
    private static boolean ehCampoCor(String n) { String s=n.toLowerCase(); return s.contains("cor")||s.contains("color"); }
    private static boolean ehCampoArquivo(String n) { String s=n.toLowerCase(); return s.contains("arquivo")||s.contains("file")||s.contains("anexo"); }
    private static boolean ehCampoTextoLongo(String n) { String s=n.toLowerCase(); return s.contains("descricao")||s.contains("observacao")||s.contains("comentario")||s.contains("mensagem")||s.contains("texto_longo"); }
    private static boolean ehCampoSlider(String n) { String s=n.toLowerCase(); return s.contains("slider")||s.contains("progresso")||s.contains("percentual")||s.contains("nivel"); }
    private static boolean ehCampoSpinner(String n) { String s=n.toLowerCase(); return s.contains("quantidade")||s.contains("qtd")||s.contains("numero")||s.contains("spinner"); }
    private static boolean ehCampoSwitch(String n) { String s=n.toLowerCase(); return s.contains("ativo")||s.contains("habilitado")||s.contains("switch")||s.contains("toggle"); }

    private static String formatarRotulo(String nomeCampo) {
        if (nomeCampo == null || nomeCampo.isBlank()) return "Campo";
        String s = nomeCampo.replace('_',' ');
        if (s.isEmpty()) return nomeCampo;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String slug(String s) {
        if (s==null) return "campo";
        return s.toLowerCase().replaceAll("[^a-z0-9]+","_");
    }
}
