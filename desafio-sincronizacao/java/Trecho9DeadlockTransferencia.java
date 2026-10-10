/*
 * Trecho9DeadlockTransferencia.java
 * Bug: a ordem de aquisicao dos monitores depende da ordem dos
 * parametros, que pode se inverter entre chamadas concorrentes.
 *
 * Compilar: javac Trecho9DeadlockTransferencia.java
 * Rodar:    java Trecho9DeadlockTransferencia
 *
 * Ha um atraso deliberado (Thread.sleep) entre os dois synchronized
 * aninhados, so para tornar o deadlock facil de observar a cada
 * execucao. Um watchdog de 5s avisa se o programa travou.
 */
public class Trecho9DeadlockTransferencia {

    static class Conta {
        int saldo;
        Conta(int saldo) { this.saldo = saldo; }
    }

    static void transferir(Conta origem, Conta destino, int valor) {

        Conta primeira, segunda;
        if (origem.getId() < destino.getId()){
            primeira = origem;
            segunda = destino;

        } else {
            primeira = destino;
            segunda = origem;
        }

        synchronized (primeira) { //o lock, ou melhor, o monitor só consegue bloquear/
            //desbloquear um recurso por vez:
            /*o lock, ou melhor, o monitor, só consegue bloquear/desbloquear um recurso por vez.
            Nessa situação, a thread 1 está usando o monitor da origem para proteger a execução do seu processo. 
            Entretanto, a thread 2 está usando o monitor do destino — que é outro monitor, de outro objeto — e 
            cada uma precisa do monitor que a outra tem.
            então a thread 1 espera até a thread 2 soltar o monitor do destino. mas a thread 1 só solta o monitor da 
            origem quando sai do bloco synchronized — e ela não sai, porque está presa dentro dele esperando o destino. 
            a thread 2 também só solta o monitor do destino quando sai do seu bloco — e também não sai, porque está presa 
            esperando a origem. então as duas ficam esperando para sempre, acontecendo o deadlock. */
            System.out.println(Thread.currentThread().getName()
                    + " pegou o lock de origem, tentando pegar o de destino...");
            try { Thread.sleep(100); //sleep = dorme e salva o synchronized.
                //wait = dorme e solta o synchronized.
                } catch (InterruptedException ignored) {}
            synchronized (segunda) {
                origem.saldo -= valor;
                destino.saldo += valor;
                System.out.println(Thread.currentThread().getName() + " completou a transferencia");

                /* o lock pertence ao objeto e nunca muda. O que muda é a ordem em que ele é capturado — e 
                essa ordem depende de quem é a origem na chamada. 
                por isso que o erro acontece: quando uma conta destino vira origem, o lock dela não muda, mas a ordem
                em que ele é pego inverte. isso faz com que duas threads possam pegar os mesmos locks em ordens opostas,
                criando uma espera circular — cada uma fica com um lock e espera o que a outra tem.
                como nenhuma solta, acontece o deadlock.

                Duas threads pegam os mesmos locks em ordens opostas: 
                T1 pega A→B, T2 pega B→A. Cada uma segura um lock e espera o da outra → ninguém solta → deadlock.

                Todas as threads pegam os locks na mesma ordem fixa (ex: menor ID primeiro): T1 e T2 pegam sempre A→B. 
                Se T2 chega depois, ela espera do lado de fora sem pegar nada → T1 termina e solta → T2 prossegue.
                Sem ciclo, sem deadlock.
 */
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Conta contaX = new Conta(1000);
        Conta contaY = new Conta(1000);

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(5000);
                System.out.println("\n*** POSSIVEL DEADLOCK: o programa nao terminou em 5s ***");
                Runtime.getRuntime().halt(1);
            } catch (InterruptedException ignored) {}
        });
        watchdog.setDaemon(true);
        watchdog.start();

        Thread t1 = new Thread(() -> transferir(contaX, contaY, 100), "thread-1 (X->Y)");
        Thread t2 = new Thread(() -> transferir(contaY, contaX, 50), "thread-2 (Y->X)");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Programa terminou normalmente (sem deadlock nesta execucao).");
    }
}
