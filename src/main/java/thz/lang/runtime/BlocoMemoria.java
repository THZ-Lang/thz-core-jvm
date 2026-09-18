package thz.lang.runtime;

import java.nio.ByteBuffer;

/**
 * <h3>Gerenciamento de Memória por Bloco Temporário</h3>
 *
 * <p>O <strong>BlocoMemoria</strong> é um alocador linear de memória temporária de altíssima performance.
 * Em vez de criar e descartar milhares de pequenos objetos individualmente no heap (o que
 * gera pausas e sobrecarga de Garbage Collection), o <em>BlocoMemoria</em> reserva um espaço contíguo
 * e atende todas as alocações avançando um contador sequencial.</p>
 *
 * <h4>Principais Vantagens:</h4>
 * <ul>
 *   <li><strong>Alocação Rápida:</strong> A alocação consiste em apenas avançar o ponteiro de deslocamento.</li>
 *   <li><strong>Limpeza Automática Instantânea:</strong> Ao término de um lote ou escopo (ex: {@code USAR_BLOCO_MEMORIA}),
 *       toda a memória utilizada é liberada de uma única vez resetando o apontador a zero.</li>
 *   <li><strong>Eficiência de Cache:</strong> Como os dados ficam contíguos no mesmo bloco, o uso de cache da CPU é maximizado.</li>
 * </ul>
 */
public final class BlocoMemoria {

    private final ByteBuffer buffer;
    private int offset = 0;
    private final int tamanhoMb;
    private final int capacidadeBytes;

    /**
     * Cria um novo bloco de memória temporária com o tamanho especificado em Megabytes (MB).
     *
     * @param tamanhoMb Capacidade total do bloco em Megabytes (ex: 1 para 1 MB, 64 para 64 MB).
     * @throws IllegalArgumentException se o tamanho for negativo.
     */
    public BlocoMemoria(int tamanhoMb) {
        if (tamanhoMb < 0) {
            throw new IllegalArgumentException("O tamanho em Megabytes (MB) do bloco de memória deve ser maior ou igual a zero.");
        }
        this.tamanhoMb = tamanhoMb;
        this.capacidadeBytes = Math.multiplyExact(tamanhoMb, 1024 * 1024);
        this.buffer = ByteBuffer.allocate(capacidadeBytes);
    }

    /**
     * Aloca um espaço contíguo de bytes dentro do bloco de memória e retorna a posição inicial.
     *
     * @param bytes Quantidade de bytes contíguos a serem alocados.
     * @return A posição (offset) inicial onde o espaço foi reservado.
     * @throws RuntimeException se a quantidade solicitada exceder a capacidade total disponível no bloco.
     */
    public int alocar(int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("A quantidade de bytes a alocar deve ser não negativa.");
        }
        int enderecoInicial = this.offset;
        if (bytes > this.capacidadeBytes - this.offset) {
            throw new RuntimeException(String.format(
                    "[Runtime THZ] Limite do bloco de memória temporária excedido: solicitado %d bytes, utilizado %d/%d bytes.",
                    bytes, this.offset, this.capacidadeBytes
            ));
        }
        this.offset += bytes;
        return enderecoInicial;
    }

    /**
     * Libera instantaneamente toda a memória alocada no bloco de uma única vez.
     */
    public void liberarTudo() {
        this.offset = 0;
    }

    /**
     * Retorna a capacidade total do bloco de memória em bytes.
     */
    public int getCapacidadeBytes() {
        return capacidadeBytes;
    }

    /**
     * Retorna a quantidade de bytes atualmente ocupados no bloco.
     */
    public int getUtilizacaoBytes() {
        return offset;
    }

    /**
     * Retorna o espaço restante ainda livre para novas alocações em bytes.
     */
    public int getEspacoLivreBytes() {
        return capacidadeBytes - offset;
    }

    /**
     * Retorna a porcentagem de memória utilizada no bloco (de 0.0% a 100.0%).
     */
    public double getPorcentagemUso() {
        if (capacidadeBytes == 0) return 0.0;
        return ((double) offset / capacidadeBytes) * 100.0;
    }

    /**
     * Indica se o bloco está completamente vazio (sem bytes alocados).
     */
    public boolean estaVazio() {
        return offset == 0;
    }

    /**
     * Retorna o tamanho configurado em Megabytes.
     */
    public int getTamanhoMb() {
        return tamanhoMb;
    }

    // --- Métodos idiomáticos adicionais ---
    public ByteBuffer getBuffer() { return buffer; }
    public ByteBuffer buffer() { return buffer; }
    public int capacidadeBytes() { return getCapacidadeBytes(); }
    public int utilizacaoBytes() { return getUtilizacaoBytes(); }
    public int espacoLivreBytes() { return getEspacoLivreBytes(); }
    public double porcentagemUso() { return getPorcentagemUso(); }
    public boolean estaVazia() { return estaVazio(); }

    @Override
    public String toString() {
        return String.format("BlocoMemoria[Utilizado: %d B / %d B (%.1f%%) — Bloco de %d MB]",
                offset, capacidadeBytes, getPorcentagemUso(), tamanhoMb);
    }
}
