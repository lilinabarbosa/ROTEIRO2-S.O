/*
 * Trecho8LostWakeup.java
 * Bug: espera condicional usa 'if' em vez de 'while', entao a thread
 * nao reavalia a condicao ao acordar.
 *
 * Compilar: javac Trecho8LostWakeup.java
 * Rodar:    java Trecho8LostWakeup
 *
 * Para forcar o bug a aparecer, ha varios produtores e consumidores
 * competindo pelo notifyAll() (mais threads acordadas ao mesmo tempo
 * aumenta a chance de uma prosseguir com uma condicao que ja nao e
 * mais verdadeira). Rode varias vezes se nao aparecer na primeira.
 */
import java.util.concurrent.atomic.AtomicInteger;

public class Trecho8LostWakeup {

    static final int TAM_BUFFER = 3;
    static final int N_PRODUTORES = 4;
    static final int N_CONSUMIDORES = 4;
    static final int ITENS_POR_PRODUTOR = 500;

    static class Buffer {
        int[] dados = new int[TAM_BUFFER];
        int inicio = 0, fim = 0, contador = 0;

        synchronized void inserir(int item) throws InterruptedException {
            while (contador == TAM_BUFFER) {          // <-- linha do bug: deveria ser 'while'
                wait(); //adormece a thread que está rodando

            }
            /*quando o contador = 3, entra no if e assim, a thread dorme, mas quando essa 
            thread acorda, ele nao verifica novamente a condição.
            isso faz com que, enquanto a thread1 dorme, a thread2 começa a executar
            e consome o buffer, quando a thread1 volta, como não verifica novamente 
            a condição, o codigo avança e ele adiciona no buffermesmo cheio, ultrapassando
            o valor de TAM_BUFFER, ocorrendo um erro.*/
            dados[fim] = item;
            fim = (fim + 1) % TAM_BUFFER;
            contador++;
            if (contador < 0 || contador > TAM_BUFFER) {
                System.out.println("*** ERRO: contador=" + contador + " fora de [0," + TAM_BUFFER + "] ***");
            }
            notifyAll(); //acorda todas as threads que estão dormmindo.
        }

        synchronized int remover() throws InterruptedException {
            while (contador == 0) {                   // <-- linha do bug: deveria ser 'while'
                wait();
            }
            int item = dados[inicio];
            inicio = (inicio + 1) % TAM_BUFFER;
            contador--;
            if (contador < 0 || contador > TAM_BUFFER) {
                System.out.println("*** ERRO: contador=" + contador + " fora de [0," + TAM_BUFFER + "] ***");
            }
            notifyAll();
            return item;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Buffer buffer = new Buffer();
        AtomicInteger proximoId = new AtomicInteger(0);
        Thread[] produtores = new Thread[N_PRODUTORES];
        Thread[] consumidores = new Thread[N_CONSUMIDORES];

        for (int i = 0; i < N_PRODUTORES; i++) {
            produtores[i] = new Thread(() -> {
                for (int j = 0; j < ITENS_POR_PRODUTOR; j++) {
                    try {
                        buffer.inserir(proximoId.getAndIncrement());
                    } catch (InterruptedException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("*** EXCECAO: " + e + " ***");
                        return;
                    }
                }
            });
            produtores[i].start();
        }
        int totalItens = N_PRODUTORES * ITENS_POR_PRODUTOR;
        for (int i = 0; i < N_CONSUMIDORES; i++) {
            consumidores[i] = new Thread(() -> {
                for (int j = 0; j < totalItens / N_CONSUMIDORES; j++) {
                    try {
                        buffer.remover();
                    } catch (InterruptedException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("*** EXCECAO: " + e + " ***");
                        return;
                    }
                }
            });
            consumidores[i].start();
        }

        for (Thread t : produtores) t.join();
        for (Thread t : consumidores) t.join();
        System.out.println("Execucao terminou. Se nenhum '*** ERRO' ou '*** EXCECAO' apareceu acima, rode de novo.");
    }
}
