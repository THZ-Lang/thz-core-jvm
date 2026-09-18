package thz.lang.governanca.liberacao;

import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.security.ThzSecurity;

import java.util.*;

/**
 * Protocolo oficial de Liberação para Produção baseado em evidências executáveis (ISO/IEC/IEEE 42010 e TR 24772).
 * Exige:
 * 1. Conformidade estática de arquitetura viva e rastreabilidade na AST;
 * 2. Homologação com 100% de sucesso, incluindo rejeição conforme de testes negativos propositais (P01);
 * 3. Ausência de pendências impeditivas em dados reais validados em modo dry-run sem gravação (P02, P03);
 * 4. Revisões vigentes unívocas para todas as regras de negócio aplicáveis (R01, R02, R03);
 * 5. Vinculação criptográfica SHA-256 ao código fonte avaliado (P04).
 */
public final class ProtocoloLiberacaoProducao {

    private ProtocoloLiberacaoProducao() {}

    public static LaudoLiberacaoProducao avaliar(SolicitacaoLiberacao solicitacao) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação de liberação não pode ser nula.");
        }

        List<String> impedimentos = new ArrayList<>();

        // 1. Identificação Criptográfica do Fonte
        String sha256Codigo = solicitacao.codigoFonte() != null
                ? ThzSecurity.sha256(solicitacao.codigoFonte())
                : "FONTE_AUSENTE";

        if (solicitacao.codigoFonte() == null || solicitacao.codigoFonte().isBlank()) {
            impedimentos.add("Código fonte ausente na solicitação de liberação.");
        }

        // 2. Auditoria Estática da AST (Arquitetura Viva)
        boolean auditoriaEstaticaAprovada = false;
        double scoreEstatico = 0.0;
        if (solicitacao.ast() != null) {
            RelatorioAuditoria relAuditoria = AuditorGovernanca.auditar(solicitacao.ast());
            scoreEstatico = relAuditoria.metricas().percentualConformidade();
            auditoriaEstaticaAprovada = relAuditoria.metricas().aprovado();
            if (!auditoriaEstaticaAprovada) {
                impedimentos.add("Auditoria estática de governança reprovada (score: " + scoreEstatico + "%).");
                for (String p : relAuditoria.metricas().pendencias()) {
                    impedimentos.add("Pendência estática: " + p);
                }
            }
        } else {
            impedimentos.add("AST do programa ausente para auditoria de arquitetura viva.");
        }

        // 3. Execução dos Casos de Homologação (Positivos e Negativos)
        List<ResultadoCasoHomologacao> resultadosCasos = new ArrayList<>();
        int casosAprovados = 0;
        int totalCasos = solicitacao.casosHomologacao() != null ? solicitacao.casosHomologacao().size() : 0;

        if (solicitacao.casosHomologacao() == null || solicitacao.casosHomologacao().isEmpty()) {
            impedimentos.add("Nenhum caso de homologação executável foi fornecido para liberação [P02].");
        } else {
            for (CasoHomologacao caso : solicitacao.casosHomologacao()) {
                try {
                    caso.executor().executar();
                    if (caso.esperaSucesso()) {
                        casosAprovados++;
                        resultadosCasos.add(new ResultadoCasoHomologacao(caso.identificador(), caso.descricao(), true, "Execução concluída com sucesso."));
                    } else {
                        // Caso negativo esperava falha, mas sucedeu -> Falha de homologação!
                        resultadosCasos.add(new ResultadoCasoHomologacao(caso.identificador(), caso.descricao(), false,
                                "Caso de teste negativo sucedeu indevidamente; esperava rejeição contendo: '" + caso.erroEsperadoContem() + "'."));
                        impedimentos.add("Caso de homologação negativo '" + caso.identificador() + "' não foi rejeitado conforme esperado [P01].");
                    }
                } catch (Throwable t) {
                    if (!caso.esperaSucesso()) {
                        String msg = t.getMessage() != null ? t.getMessage() : t.toString();
                        if (caso.erroEsperadoContem() == null || msg.contains(caso.erroEsperadoContem())) {
                            casosAprovados++;
                            resultadosCasos.add(new ResultadoCasoHomologacao(caso.identificador(), caso.descricao(), true,
                                    "Rejeitado conforme esperado: " + msg));
                        } else {
                            resultadosCasos.add(new ResultadoCasoHomologacao(caso.identificador(), caso.descricao(), false,
                                    "Rejeitado com mensagem divergente do esperado. Esperava conter: '" + caso.erroEsperadoContem() + "', obtido: " + msg));
                            impedimentos.add("Caso de homologação '" + caso.identificador() + "' divergiu da rejeição esperada.");
                        }
                    } else {
                        // Caso positivo falhou -> Falha de homologação!
                        resultadosCasos.add(new ResultadoCasoHomologacao(caso.identificador(), caso.descricao(), false,
                                "Erro inesperado na execução: " + t.getMessage()));
                        impedimentos.add("Caso de homologação '" + caso.identificador() + "' falhou: " + t.getMessage());
                    }
                }
            }
        }
        boolean homologacaoAprovada = totalCasos > 0 && casosAprovados == totalCasos;

        // 4. Validação em Dados Reais (Modo Dry-Run)
        boolean dadosReaisAprovados = false;
        RegistroDadosReaisValidados dados = solicitacao.dadosReais();
        if (dados == null) {
            impedimentos.add("Avaliação contra dados reais de validação ausente [P02].");
        } else {
            if (!dados.modoValidacaoSemGravacao()) {
                impedimentos.add("A validação com dados reais violou a política de acervo: não pode gravar no banco durante homologação [P03].");
            }
            if (dados.totalPendenciasImpeditivas() > 0) {
                impedimentos.add("Dados reais avaliados possuem " + dados.totalPendenciasImpeditivas() + " pendência(s) impeditiva(s) [P02].");
                for (String pend : dados.pendencias()) {
                    impedimentos.add("Pendência de dados: " + pend);
                }
            }
            dadosReaisAprovados = dados.modoValidacaoSemGravacao() && dados.totalPendenciasImpeditivas() == 0;
        }

        // 5. Vigência e Revisão de Regras
        boolean regrasVigentesAprovadas = true;
        if (solicitacao.ast() != null && solicitacao.ast().regras() != null) {
            for (var regra : solicitacao.ast().regras()) {
                String rev = solicitacao.revisoesRegrasVigentes() != null
                        ? solicitacao.revisoesRegrasVigentes().get(regra.nome())
                        : null;
                if (rev == null || rev.isBlank()) {
                    regrasVigentesAprovadas = false;
                    impedimentos.add("Regra '" + regra.nome() + "' não possui revisão vigente unívoca associada [R01, R02, R03].");
                }
            }
        }

        // Veredito Final
        boolean liberadoParaProducao = auditoriaEstaticaAprovada
                && homologacaoAprovada
                && dadosReaisAprovados
                && regrasVigentesAprovadas
                && impedimentos.isEmpty();

        String versao = solicitacao.ast() != null && solicitacao.ast().versaoLinguagem() != null
                ? solicitacao.ast().versaoLinguagem()
                : thz.lang.version.ThzVersion.ATUAL.toString();

        return new LaudoLiberacaoProducao(
                solicitacao.nomePrograma(),
                sha256Codigo,
                versao,
                solicitacao.referenciaTemporal() != null ? solicitacao.referenciaTemporal() : java.time.Instant.now().toString(),
                auditoriaEstaticaAprovada,
                scoreEstatico,
                homologacaoAprovada,
                totalCasos,
                casosAprovados,
                List.copyOf(resultadosCasos),
                dadosReaisAprovados,
                dados,
                regrasVigentesAprovadas,
                liberadoParaProducao,
                List.copyOf(impedimentos)
        );
    }
}
