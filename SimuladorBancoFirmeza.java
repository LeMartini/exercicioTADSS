import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

public class SimuladorBancoFirmeza {

    static class Cliente {
        long chegada, inicio, fim;
        Cliente(long chegada) { this.chegada = chegada; }
        long espera() { return inicio - chegada; }
        long atendimento() { return fim - inicio; }
        long total() { return fim - chegada; }
    }

    public static void main(String[] args) throws InterruptedException {
        final int DURACAO_REAL = 2 * 60 * 60 * 1000; // 2 horas
        final int DURACAO_SIMULADA = 10 * 1000; // 10 segundos

        final int CHEGADA_MIN_REAL = 5000;
        final int CHEGADA_MAX_REAL = 50000;
        final int ATEND_MIN_REAL = 30000;
        final int ATEND_MAX_REAL = 120000;

        double fatorTempo = (double) DURACAO_SIMULADA / DURACAO_REAL;

        int CHEGADA_MIN = (int) (CHEGADA_MIN_REAL * fatorTempo);
        int CHEGADA_MAX = (int) (CHEGADA_MAX_REAL * fatorTempo);
        int ATEND_MIN = (int) (ATEND_MIN_REAL * fatorTempo);
        int ATEND_MAX = (int) (ATEND_MAX_REAL * fatorTempo);

        BlockingQueue<Cliente> fila = new LinkedBlockingQueue<>();
        List<Cliente> atendidos = Collections.synchronizedList(new ArrayList<>());
        long inicioSimulacao = System.currentTimeMillis();

        Runnable criarAtendente = () -> {
            try {
                while (System.currentTimeMillis() - inicioSimulacao < DURACAO_SIMULADA || !fila.isEmpty()) {
                    Cliente c = fila.poll(1, TimeUnit.SECONDS);
                    if (c != null) {
                        c.inicio = System.currentTimeMillis();
                        int duracao = ThreadLocalRandom.current().nextInt(ATEND_MIN, ATEND_MAX + 1);
                        Thread.sleep(duracao);
                        c.fim = System.currentTimeMillis();
                        atendidos.add(c);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // 4 atendentes
        Thread atendente1 = new Thread(criarAtendente);
        Thread atendente2 = new Thread(criarAtendente);
        Thread atendente3 = new Thread(criarAtendente);
        Thread atendente4 = new Thread(criarAtendente);

        Thread gerador = new Thread(() -> {
            try {
                while (System.currentTimeMillis() - inicioSimulacao < DURACAO_SIMULADA) {
                    // Garantir que os clientes sejam gerados com frequência
                    Thread.sleep(ThreadLocalRandom.current().nextInt(CHEGADA_MIN, CHEGADA_MAX + 1));
                    fila.add(new Cliente(System.currentTimeMillis()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        gerador.start();
        atendente1.start();
        atendente2.start();
        atendente3.start();
        atendente4.start();

        gerador.join();
        atendente1.join();
        atendente2.join();
        atendente3.join();
        atendente4.join();

        int total = atendidos.size();
        long maxEspera = atendidos.stream().mapToLong(Cliente::espera).max().orElse(0);
        long maxAtend = atendidos.stream().mapToLong(Cliente::atendimento).max().orElse(0);
        double mediaTotal = atendidos.stream().mapToLong(Cliente::total).average().orElse(0);
        double mediaEspera = atendidos.stream().mapToLong(Cliente::espera).average().orElse(0);

        double maxEsperaReal = maxEspera / fatorTempo;
        double maxAtendReal = maxAtend / fatorTempo;
        double mediaTotalReal = mediaTotal / fatorTempo;
        double mediaEsperaReal = mediaEspera / fatorTempo;

        System.out.println("----- SIMULAÇÃO COM 4 ATENDENTES (Tempo Real Simulado de 2h) -----");
        System.out.println("Clientes atendidos: " + total);
        System.out.println("Tempo máx espera: " + formatarTempo((long) maxEsperaReal));
        System.out.println("Tempo máx atendimento: " + formatarTempo((long) maxAtendReal));
        System.out.println("Tempo médio total: " + formatarTempo((long) mediaTotalReal));
        System.out.println("Tempo médio espera: " + formatarTempo((long) mediaEsperaReal));
        System.out.println("Objetivo alcançado (<2min)? " + ((mediaEsperaReal <= 120_000) ? "Sim ✅" : "Não ❌"));
    }

    // Formata milissegundos para HH:mm:ss
    private static String formatarTempo(long millis) {
        long segundos = millis / 1000;
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long segRestantes = segundos % 60;
        return String.format("%02dh %02dm %02ds", horas, minutos, segRestantes);
    }
}
