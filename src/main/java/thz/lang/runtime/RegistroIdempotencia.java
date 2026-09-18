package thz.lang.runtime;

import thz.lang.interpretador.ValorThz;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registro de Idempotência Inteligente de Larga Escala (Wide-Language Application).
 * <p>
 * Garante que invocações repetidas de operações ou regras de negócio marcadas como
 * {@code IDEMPOTENTE} com o mesmo conjunto de argumentos ou chave determinística
 * retornem o mesmo resultado transacional em tempo constante $O(1)$, evitando
 * re-execução redundante, sobrecarga de processamento e duplicação de efeitos colaterais.
 */
public final class RegistroIdempotencia {

    public record EntradaIdempotencia(
            String chave,
            String operacao,
            ValorThz resultado,
            long timestampCriacao,
            long timestampUltimoAcesso,
            int execucoesEvitadas
    ) {}

    private final Map<String, EntradaIdempotencia> cache = new ConcurrentHashMap<>();
    private final AtomicInteger contadorExecucoesEvitadas = new AtomicInteger(0);
    private final AtomicInteger contadorNovasExecucoes = new AtomicInteger(0);

    public RegistroIdempotencia() {}

    /**
     * Computa uma chave SHA-256 determinística para uma operação e seus argumentos formatados.
     */
    public static String computarChaveDeterministica(String operacao, Map<String, ValorThz> argumentos) {
        StringBuilder sb = new StringBuilder(operacao != null ? operacao : "OP").append("::");
        if (argumentos != null && !argumentos.isEmpty()) {
            List<String> chaves = new ArrayList<>(argumentos.keySet());
            Collections.sort(chaves);
            for (String k : chaves) {
                ValorThz v = argumentos.get(k);
                sb.append(k).append("=").append(v != null ? v.toString() : "NULO").append(";");
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return (operacao != null ? operacao : "OP") + "-" + hex.substring(0, 16);
        } catch (Exception e) {
            return (operacao != null ? operacao : "OP") + "-" + Math.abs(sb.toString().hashCode());
        }
    }

    /**
     * Executa a operação fornecida ou reutiliza o resultado previamente registrado para a chave de idempotência.
     */
    public ValorThz executarOuReutilizar(
            String operacao,
            String chaveExplicita,
            Map<String, ValorThz> argumentos,
            Supplier<ValorThz> executor,
            Consumer<String> logger
    ) {
        String chaveFinal = (chaveExplicita != null && !chaveExplicita.isBlank())
                ? chaveExplicita
                : computarChaveDeterministica(operacao, argumentos);

        EntradaIdempotencia existente = cache.get(chaveFinal);
        if (existente != null) {
            contadorExecucoesEvitadas.incrementAndGet();
            EntradaIdempotencia atualizada = new EntradaIdempotencia(
                    existente.chave(),
                    existente.operacao(),
                    existente.resultado(),
                    existente.timestampCriacao(),
                    System.currentTimeMillis(),
                    existente.execucoesEvitadas() + 1
            );
            cache.put(chaveFinal, atualizada);

            if (logger != null) {
                logger.accept("[IDEMPOTÊNCIA] Reutilização de resultado idêntico para chave '" + chaveFinal + "' em " + operacao + "() [execução evitada ✓]");
            }
            return existente.resultado();
        }

        // Primeira execução
        contadorNovasExecucoes.incrementAndGet();
        ValorThz resultado = executor.get();

        long agora = System.currentTimeMillis();
        EntradaIdempotencia novaEntrada = new EntradaIdempotencia(
                chaveFinal,
                operacao,
                resultado,
                agora,
                agora,
                0
        );
        cache.put(chaveFinal, novaEntrada);

        return resultado;
    }

    /**
     * Limpa todo o cache de idempotência em tempo constante $O(1)$.
     */
    public void limpar() {
        cache.clear();
        contadorExecucoesEvitadas.set(0);
        contadorNovasExecucoes.set(0);
    }

    public boolean contemChave(String chave) {
        return chave != null && cache.containsKey(chave);
    }

    public EntradaIdempotencia obterEntrada(String chave) {
        return cache.get(chave);
    }

    public int getTotalRegistros() {
        return cache.size();
    }

    public int getContadorExecucoesEvitadas() {
        return contadorExecucoesEvitadas.get();
    }

    public int getContadorNovasExecucoes() {
        return contadorNovasExecucoes.get();
    }

    public List<EntradaIdempotencia> listarEntradas() {
        return List.copyOf(cache.values());
    }

    /**
     * Gera um sumário de estatísticas de idempotência e economia de processamento.
     */
    public String gerarRelatorioEstatisticas() {
        int total = getTotalRegistros();
        int evitadas = getContadorExecucoesEvitadas();
        int novas = getContadorNovasExecucoes();
        int totalChamadas = novas + evitadas;
        double taxaReuso = totalChamadas > 0 ? ((double) evitadas / totalChamadas) * 100.0 : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================\n");
        sb.append("   RELATÓRIO DE IDEMPOTÊNCIA TRANSAÇÃO / RUNTIME\n");
        sb.append("===============================================================\n");
        sb.append(String.format("Chaves Únicas Registradas:    %d\n", total));
        sb.append(String.format("Novas Execuções Primárias:    %d\n", novas));
        sb.append(String.format("Re-execuções Evitadas (Hit):  %d\n", evitadas));
        sb.append(String.format("Taxa de Reuso / Economia:     %.2f%%\n", taxaReuso));
        sb.append("---------------------------------------------------------------\n");
        if (total == 0) {
            sb.append("(Nenhum registro de idempotência acumulado)\n");
        } else {
            for (EntradaIdempotencia e : cache.values()) {
                sb.append(String.format(" • [%s] %s -> Reusado %d vez(es)\n", e.chave(), e.operacao(), e.execucoesEvitadas()));
            }
        }
        sb.append("===============================================================\n");
        return sb.toString();
    }
}
